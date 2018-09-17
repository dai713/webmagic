package cn.mc.scheduler.crawler.sports.eastday.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.utils.CollectionUtil;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.crawler.lanxiongsports.com.LanXiongSportsCrawlers;
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import cn.mc.scheduler.util.RedisUtil;
import cn.mc.scheduler.util.SchedulerUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 东方体育新闻
 *
 * @author daiqingwen
 * @date 2018/7/30 下午 15:41
 */

@Component
public class EastSportsCrawlers extends BaseCrawler {

    private static final Logger log = LoggerFactory.getLogger(LanXiongSportsCrawlers.class);

    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(1000);

    private Long temp = System.currentTimeMillis();

    private String url = "http://dfsports_h5.dftoutiao.com/dfsports_h5/newspool?type=tuijian&" +
            "typecode=901215&startkey=2653659865412658359&newkey=&pgnum=3&os=Android+6.0&recgid=" +
            "15326759474123007&qid=baiducom&domain=dfsports_h5&readhistory=180726233800187000000%" +
            "2C180727180644447000000%2C180730074033442000000&_="+ temp +"&callback=Zepto"+ temp +"";

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private SchedulerUtils schedulerUtils;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private EastSportsPipeline eastSportsPipeline;

    private List<NewsDO> list = Lists.newArrayList();
//    private List<NewsContentArticleDO> detailList = Lists.newArrayList();

    private Map<String, NewsDO> newsMap = Maps.newHashMap();
    private Map<String, List<NewsImageDO>> imageMap = Maps.newHashMap();
    private String eastSports = "EASTSPORTS";


    @Override
    public Spider createCrawler() {
        Request request = new Request();
        request.setUrl(url);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public void process(Page page) {
        if (url.equals(page.getUrl().toString())) {
            getList(page);
        } else {
            getDetails(page);
        }

    }

    @Override
    public Site getSite() {
        return SITE;
    }

    /**
     * 获取新闻列表
     * @param page
     */
    private void getList(Page page){
        log.info("开始抓取东方体育新闻...");
        try {
            String rawText = page.getRawText();
            String substring = rawText.substring(rawText.indexOf("(") + 1, rawText.length() - 1);
            JSONObject jsonObject = JSON.parseObject(substring);
            JSONArray data = jsonObject.getJSONArray("data");
            String cache = (String) redisUtil.get(eastSports);
            List<String> urlList = Lists.newArrayList();

            for (int i = 0; i < data.size(); i++) {
                JSONObject obj = data.getJSONObject(i);
                String rowkey = String.valueOf(obj.getLong("rowkey"));
                Integer isvideo = obj.getInteger("isvideo");
                if (0 != isvideo) {
                    continue;
                }
                String key = this.encrypt(rowkey);
                if (!StringUtils.isEmpty(cache)) {
                    boolean matchedResult = schedulerUtils.getCacheData(cache, key);
                    if (matchedResult) {
                        continue;
                    }
                }

                Long newId = IDUtil.getNewID();
                Date date = new Date();
                String title = obj.getString("topic");
                Integer hot = obj.getInteger("ishot");
                String url = obj.getString("url");
                String source = obj.getString("source");
                String keywords = obj.getString("tags");
                Integer commend = obj.getInteger("iscommend");
                Date displayTime = DateUtil.parse(obj.getString("date"), DateUtil.DATE_FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND2);
                Integer displayType = 0;
                JSONArray imgArray = obj.getJSONArray("miniimg");
                String dataKey = this.encrypt(url);

                if (!CollectionUtil.isEmpty(imgArray)) {
                    if (imgArray.size() > 0) {
                        displayType = NewsDO.DISPLAY_TYPE_THREE_MINI_IMAGE;
                    } else {
                        displayType = NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE;
                    }
                }

                urlList.add(url);
                NewsDO newsDO = newsCoreManager.buildNewsDO(newId, dataKey, title,
                        hot, url, "", source, url,
                        NewsDO.NEWS_TYPE_SPORTS, NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                        keywords, 0, "", displayTime,
                        0, 0, date, displayType, commend, commend);
                list.add(newsDO);
                newsMap.put(dataKey, newsDO);

                List<NewsImageDO> imageList = Lists.newArrayList();

                for (int j = 0; j < imgArray.size(); j++) {
                    JSONObject img = imgArray.getJSONObject(j);
                    Integer srcWidth = img.getInteger("imgwidth");
                    Integer srcHeight = img.getInteger("imgheight");
                    String imgUrl = img.getString("src");
                    // 上传图片至阿里云
                    String saveImg = aliyunOSSClientUtil.uploadPicture(imgUrl);

                    NewsImageDO newsImage = new NewsImageDO(IDUtil.getNewID(), newId, srcWidth,
                            srcHeight, NewsImageDO.IMAGE_TYPE_MINI, saveImg, NewsImageDO.STATUS_NORMAL);
                    imageList.add(newsImage);
                }

                imageMap.put(dataKey, imageList);


            }

            redisUtil.remove(eastSports);
            redisUtil.setString(eastSports, JSON.toJSONString(list));
            page.addTargetRequests(urlList);

        } catch (Exception e) {
            log.error("抓取东方新闻失败：{}", e);
        }
    }

    /**
     * 获取新闻详情
     * @param page
     */
    private void getDetails(Page page) {
        String content = page.getHtml().xpath("//div[@id='content']").toString();
        String originUrl = page.getUrl().toString();
        content = schedulerUtils.replaceLabel(content);
        String dataKey = this.encrypt(originUrl);

        NewsContentArticleDO contentArticleDO = new NewsContentArticleDO(IDUtil.getNewID(), null, content, NewsContentArticleDO.ARTICLE_TYPE_HTML);

        // 保存
        eastSportsPipeline.save(dataKey, newsMap, imageMap, contentArticleDO);
    }



}
