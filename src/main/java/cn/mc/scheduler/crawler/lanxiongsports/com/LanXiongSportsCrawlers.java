package cn.mc.scheduler.crawler.lanxiongsports.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.utils.CollectionUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
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
import us.codecraft.webmagic.selector.Selectable;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 懒熊网新闻
 *
 * @author daiqingwen
 * @date 2018/7/26 下午 17:46
 */

@Component
public class LanXiongSportsCrawlers extends BaseCrawler {

    private static final Logger log = LoggerFactory.getLogger(LanXiongSportsCrawlers.class);

    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(1000);

    private Long temp = System.currentTimeMillis();

    private String url = "http://www.lanxiongsports.com/mservice/?c=news&a=index&format=json&_=" + temp;

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private SchedulerUtils schedulerUtils;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private LanXiongSportsPipeline lanXiongSportsPipeline;

    private List<NewsDO> list = Lists.newArrayList();
    private Map<String, NewsDO> newsMap = Maps.newHashMap();
    private Map<String, NewsImageDO> imageMap = Maps.newHashMap();
    private String lanxiong = "LANXIONG";


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
    private void getList(Page page) {
        log.info("开始抓取懒熊体育新闻...");
        try {
            JSONObject jsonObject = JSON.parseObject(page.getRawText());
            JSONArray data = jsonObject.getJSONArray("items");
            String cache = (String) redisUtil.get(lanxiong);
            List<String> urlList = Lists.newArrayList();

            for (int i = 0; i < data.size(); i++) {
                JSONObject obj = data.getJSONObject(i);
                JSONObject category = obj.getJSONObject("_category");
                if (StringUtils.isEmpty(category)) {
                    continue;
                }
                if (category.getString("name").equals("推广")) {
                    continue;
                }
                String id = String.valueOf(obj.getInteger("id"));
                String key = this.encrypt(id);
                if (!StringUtils.isEmpty(cache)) {
                    boolean matchedResult = schedulerUtils.getCacheData(cache, key);
                    if (matchedResult) {
                        continue;
                    }
                }
                String imgUrl = obj.getString("logo");
                String title = obj.getString("title");
                String newsAbstract = obj.getString("summary");
                String url = obj.getString("url");
                Long newId = IDUtil.getNewID();
                String dataKey = this.encrypt(url);
                Date date = new Date();
                Integer srcWidth;
                Integer srcHeight;
                Map map = schedulerUtils.parseImg(imgUrl);
                if (StringUtils.isEmpty(map)) {
                    srcHeight = 0;
                    srcWidth = 0;
                } else {
                    srcWidth = (Integer) map.get("width");
                    srcHeight = (Integer) map.get("height");
                }
                urlList.add(url);

                NewsDO newsDO = newsCoreManager.buildNewsDO(newId, dataKey, title,
                        0, url, "", NewsDO.DATA_SOURCE_LANXIONG, url,
                        NewsDO.NEWS_TYPE_SPORTS, NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                        "", 0, newsAbstract, date,0,
                        0, date, NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE, 0, 0);
                list.add(newsDO);
                newsMap.put(dataKey, newsDO);

                // 上传图片至阿里云
                String saveImg = aliyunOSSClientUtil.uploadPicture(url);
//                String saveImg = "";

                NewsImageDO newsImage = new NewsImageDO(IDUtil.getNewID(), newId, srcWidth,
                        srcHeight, NewsImageDO.IMAGE_TYPE_LARGE, saveImg, NewsImageDO.STATUS_NORMAL);
//                imageList.add(newsImage);
                imageMap.put(dataKey, newsImage);

            }

            if (CollectionUtil.isEmpty(newsMap)) {
                return;
            }
            redisUtil.remove(lanxiong);
            redisUtil.setString(lanxiong, JSON.toJSONString(list));
            page.addTargetRequests(urlList);

        } catch (Exception e){
            log.error("抓取懒熊体育新闻失败:{}", e);
        }
    }


    /**
     * 获取新闻详情
     * @param page
     */
    private void getDetails(Page page) {
        String originUrl = page.getUrl().toString();
        String cla = page.getHtml().xpath("//div[@class='imagecontent']/@class").toString();
        String style = page.getHtml().xpath("//div[@class='imagecontent']/@style").toString();
        String dataKey = this.encrypt(originUrl);

        // 拼接文章内容
        String div = "<div class='" + cla + "' style='" + style + "'>";
        List<Selectable> nodes = page.getHtml().xpath("//div[@class='imagecontent']/p").nodes();
        StringBuilder sb = new StringBuilder(div);
        int count = nodes.size() - 8;
        for (int i = 0; i < nodes.size(); i++) {
            String lable = nodes.get(i).toString();
            if (i > count) {
                break;
            }
            sb.append(lable);
        }
        sb.append("</div>");
        // 替换所有<a>标签
        String content = schedulerUtils.replaceLabel(sb.toString());

        NewsContentArticleDO contentArticleDO = new NewsContentArticleDO(IDUtil.getNewID(), null, content, NewsContentArticleDO.ARTICLE_TYPE_HTML);

        lanXiongSportsPipeline.save(dataKey, newsMap, imageMap, contentArticleDO);


    }

}
