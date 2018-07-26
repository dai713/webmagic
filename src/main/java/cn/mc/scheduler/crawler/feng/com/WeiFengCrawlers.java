package cn.mc.scheduler.crawler.feng.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
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
 * 威锋网新闻
 *
 * @author daiqingwen
 * @date 2018/7/24 上午 9:46
 */

@Component
public class WeiFengCrawlers extends BaseCrawler {

    private static final Logger log = LoggerFactory.getLogger(WeiFengCrawlers.class);

    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(100);

    private static final String url = "http://www.feng.com/publish/bbsapp.php" +
            "?r=api/dyn&v=2&type=getArticlesByType";

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private SchedulerUtils schedulerUtils;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private WeiFengPipeline weiFengPipeline;

    private List<NewsDO> list = Lists.newArrayList();
    private List<NewsContentArticleDO> detailList = Lists.newArrayList();
    private List<NewsImageDO> imageList = Lists.newArrayList();

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
        JSONObject jsonObject = JSON.parseObject(page.getRawText());
        JSONObject data = jsonObject.getJSONObject("data");
        JSONArray array = data.getJSONArray("dataList");
        JSONArray array1 = new JSONArray();

        // 获取第一条新闻
        String firstId = String.valueOf(array.getJSONObject(0).getInteger("jumpNext"));
        String firstUrl = array.getJSONObject(0).getString("url");
        String cache = (String) redisUtil.get(firstId);
        if(!StringUtils.isEmpty(cache)) {
            if (firstUrl.equals(cache)) {
                return;
            } else {
                redisUtil.setString(firstId, firstUrl);
            }
        } else {
            redisUtil.setString(firstId, firstUrl);
        }

        List<String> urlList = Lists.newArrayList();

        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            Long newId = IDUtil.getNewID();
            String dataKey = this.encrypt(String.valueOf(IDUtil.getNewID()));
            Date date = new Date();
            String picture = obj.getString("picture");
            String id = String.valueOf(obj.getInteger("jumpNext"));
            String title = obj.getString("title");
            Integer srcWidth;
            Integer srcHeight;
            Map map = schedulerUtils.parseImg(picture);
            if (StringUtils.isEmpty(map)) {
                srcHeight = 0;
                srcWidth = 0;
            } else {
                srcWidth = (Integer) map.get("width");
                srcHeight = (Integer) map.get("height");
            }

            String detailUrl = "https://bbs.feng.com/mobile-news-read-0-";
            String url = detailUrl + id + ".html";
            urlList.add(url);
            NewsDO newsDO = newsCoreManager.buildNewsDO(newId, dataKey, title,
                    0, url, "", NewsDO.DATA_SOURCE_WEIFENG, url,
                    NewsDO.NEWS_TYPE_TECHNOLOGY, NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                    "", 0, obj.getString("message"), date,
                    0, 0, date, NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE,
                    obj.getInteger("replies"), obj.getInteger("replies"));
            list.add(newsDO);

            // 上传图片至阿里云
            String saveImg = aliyunOSSClientUtil.uploadPicture(picture);
            NewsImageDO newsImage = new NewsImageDO(IDUtil.getNewID(), newId, srcWidth,
                    srcHeight, NewsImageDO.IMAGE_TYPE_MINI, saveImg, NewsImageDO.STATUS_NORMAL);
            imageList.add(newsImage);
        }

        page.addTargetRequests(urlList);
    }

    /**
     * 获取新闻详情
     * @param page
     */
    private void getDetails(Page page) {
        String content = page.getHtml().xpath("//article[@class='article']").toString();

        content = schedulerUtils.replaceLabel(content);

        String originUrl = page.getUrl().toString();
        originUrl = originUrl.substring(originUrl.lastIndexOf("-")+1, originUrl.length() - 5);
        Long newsId = 0L;
        for (NewsDO newsDO : list) {
            String newsUrl = newsDO.getNewsUrl();
            newsUrl = newsUrl.substring(newsUrl.lastIndexOf("-")+1, newsUrl.length() - 5);
            if (originUrl.equals(newsUrl)) {
                newsId = newsDO.getNewsId();
                break;
            }
        }
        NewsContentArticleDO contentArticleDO = new NewsContentArticleDO(IDUtil.getNewID(), newsId, content, NewsContentArticleDO.ARTICLE_TYPE_HTML);
        detailList.add(contentArticleDO);

        if (list.size() == detailList.size()) {
            weiFengPipeline.save(list, detailList, imageList);
        }

    }
}
