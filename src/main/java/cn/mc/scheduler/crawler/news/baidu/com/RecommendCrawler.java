package cn.mc.scheduler.crawler.news.baidu.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsContentArticleCoreManager;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.crawler.CrawlerManager;
import cn.mc.scheduler.mapper.NewsContentArticleMapper;
import cn.mc.scheduler.mapper.NewsImageMapper;
import cn.mc.scheduler.mapper.NewsMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.model.HttpRequestBody;
import us.codecraft.webmagic.utils.HttpConstant;

import java.util.*;

/**
 * @auther sin
 * @time 2018/3/8 09:36
 */
@Component
public class RecommendCrawler extends BaseCrawler {

    /**
     * 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
     */
    private Site site = Site.me().setRetryTimes(3).setSleepTime(200);

    private static final String URL = "https://news.baidu.com/news?tn=bdapibaiyue&t=newchosenlist";
    private static final String URL_INFO = "https://news.baidu.com/news?tn=bdapibaiyue&t=recommendinfo";

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsContentArticleCoreManager newsContentArticleCoreManager;
    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsImageMapper newsImageMapper;
    @Autowired
    private NewsContentArticleMapper newsContentArticleMapper;
    @Autowired
    private CrawlerManager crawlerManager;

    private Map<String, NewsDO> cacheNewsData;
    private Map<String, List<NewsImageDO>> cacheNewsImageData;

    @Override
    public Spider createCrawler() {
        Request request = new Request();

        // set cookie
        setCookie(request);

        // header
        setHeader(request);

        Map<String, Object> bodyParams = new LinkedHashMap<>();
        bodyParams.put("mid", "A7C60D2C1A763B7458FA459BECB09A4B:FG=1");
        bodyParams.put("cuid", "");
        bodyParams.put("ln", "18");
        bodyParams.put("wf", "0");
        bodyParams.put("action", "0");
        bodyParams.put("down", "0");
//        bodyParams.put("display_time", "1520653416456");
        bodyParams.put("display_time", System.currentTimeMillis() - 3600000);
        bodyParams.put("withtoppic", "1");
        bodyParams.put("orientation", "1");
        bodyParams.put("from", "news_webapp");
        bodyParams.put("pd", "webapp");
        bodyParams.put("os", "iphone");
        bodyParams.put("nids", "");
        bodyParams.put("remote_device_type", "1");
        bodyParams.put("os_type", "2");
        bodyParams.put("screen_size_width", "375");
        bodyParams.put("screen_size_height", "667");

        String encoding = "UTF-8";
        request.setUrl(URL);
        request.setMethod(HttpConstant.Method.POST);

        HttpRequestBody requestBody = HttpRequestBody.form(bodyParams, encoding);
        request.setRequestBody(requestBody) ;

        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public synchronized void process(Page page) {

        JSONObject jsonObject = JSON.parseObject(page.getRawText());
        if (!String.valueOf(jsonObject.get("errno")).equals("0")) {
            return;
        }

        JSONObject dataJsonObject = (JSONObject) jsonObject.get("data");

        String requestUrl = page.getUrl().toString();
        if (requestUrl.equals(URL)) {

            // 百度分为两个 新闻提，一个是 topPic 和 news
            JSONArray newsDataJsonArray = (JSONArray) dataJsonObject.get("news");
            if (CollectionUtils.isEmpty(newsDataJsonArray)) {
                newsDataJsonArray = (JSONArray) dataJsonObject.get("toppic");
            }
            if (CollectionUtils.isEmpty(newsDataJsonArray)) {
                return;
            }

            JSONArray newsJsonArray = newsDataJsonArray;
            // 初始化 cache
            if (CollectionUtils.isEmpty(cacheNewsData)) {
                cacheNewsData = new LinkedHashMap<>(newsJsonArray.size());
            }
            if (CollectionUtils.isEmpty(cacheNewsImageData)) {
                cacheNewsImageData = new LinkedHashMap<>(0);
            }

            for (Object newsArrayItem : newsJsonArray) {
                JSONObject newsObject = (JSONObject) newsArrayItem;
                Long newsId = IDUtil.getNewID();
                String title = String.valueOf(newsObject.get("title"));
                String abs = String.valueOf(newsObject.get("abs"));
                String longAbs = String.valueOf(newsObject.get("long_abs"));
                Date displayTime = new Date(Long.valueOf(newsObject.get("ts").toString()));
                Date createTime = new Date(Long.valueOf(newsObject.get("sourcets").toString()));
                String newSource = String.valueOf(newsObject.get("site"));
                String newSourceUrl = String.valueOf(newsObject.get("display_url"));
                String dataKey = String.valueOf(newsObject.get("nid"));

                Object imageUrlsObject = newsObject.get("imageurls");
                List<NewsImageDO> newsImageDOList = Collections.emptyList();
                if (imageUrlsObject != null) {
                    JSONArray imageUrlsJsonArray = (JSONArray) imageUrlsObject;

                    newsImageDOList = new ArrayList<>(imageUrlsJsonArray.size());
                    for (Object imageUrlObject : imageUrlsJsonArray) {
                        JSONObject imageUrlJsonObject = (JSONObject) imageUrlObject;

                        String url = String.valueOf(imageUrlJsonObject.get("url"));
                        Integer height = Integer.valueOf(imageUrlJsonObject.get("height").toString());
                        Integer width = Integer.valueOf(imageUrlJsonObject.get("width").toString());

                        newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                                IDUtil.getNewID(), newsId, url, width,
                                height, NewsImageDO.IMAGE_TYPE_MINI));

                    }
                }

                String newsUrl = "";
                String shareUrl = "";
                int imageCount = newsImageDOList.size();
                Integer displayType = handleNewsDisplayType(imageCount);
                NewsDO newsDO = newsCoreManager.buildNewsDO(newsId, dataKey, title,
                        0, newsUrl, shareUrl, newSource, newSourceUrl,
                        NewsDO.NEWS_TYPE_HEADLINE, NewsDO.CONTENT_TYPE_IMAGE_TEXT, "",
                        0, abs, displayTime, 0, imageCount, createTime,
                        displayType, 0, 0);

                // cache
                cacheNewsData.put(dataKey, newsDO);
                cacheNewsImageData.put(dataKey, newsImageDOList);

                // add url
                Request request = new Request(URL_INFO);
                setHeader(request);
                setCookie(request);

                Map<String, Object> params = new LinkedHashMap<>();
                params.put("cuid", "");
                params.put("nids", dataKey);
                params.put("wf", "1");
                params.put("remote_device_type", "1");
                params.put("os_type", "2");
                params.put("screen_size_width", "375");
                params.put("screen_size_height", "667");

                request.setRequestBody(HttpRequestBody.form(params, "UTF-8"));
                request.setMethod(HttpConstant.Method.POST);
                page.addTargetRequest(request);
            }
        } else if (requestUrl.equals(URL_INFO)) {

            JSONArray newsJsonArray = (JSONArray) dataJsonObject.get("news");
            if (CollectionUtils.isEmpty(newsJsonArray)) {
                return;
            }

            JSONObject newsDataJsonObject = (JSONObject) newsJsonArray.get(0);
            String dataKey = String.valueOf(newsDataJsonObject.get("nid"));

            if (!cacheNewsData.containsKey(dataKey)) {
                return;
            }

            JSONArray contentJsonArray = (JSONArray) newsDataJsonObject.get("content");

            StringBuilder stringBuilder = new StringBuilder();

            String imageTag = "<image src='%s' />";
            String textTag = "<p>%s</p>";
            for (Object contentObject : contentJsonArray) {
                JSONObject contentJsonObject = (JSONObject) contentObject;
                String type = String.valueOf(contentJsonObject.get("type"));
                if (type.equals("image")) {
                    JSONObject imageDataJsonObject = (JSONObject) contentJsonObject.get("data");
                    JSONObject bigImageJsonObject = (JSONObject) imageDataJsonObject.get("big");

                    String url = String.valueOf(bigImageJsonObject.get("url"));
//                    Integer width = Integer.valueOf(bigImageJsonObject.get("width").toString());
//                    Integer height = Integer.valueOf(bigImageJsonObject.get("height").toString());
                    stringBuilder.append(String.format(imageTag, url));
                    stringBuilder.append("\r\n");
                } else if (type.equals("text")) {
                    String textData = String.valueOf(contentJsonObject.get("data"));
                    stringBuilder.append(String.format(textTag, textData));
                    stringBuilder.append("\r\n");
                }
            }

            String article = stringBuilder.toString();
            NewsDO newsDO = cacheNewsData.get(dataKey);
            List<NewsImageDO> newsImageDOList = cacheNewsImageData.get(dataKey);
            NewsContentArticleDO newsContentArticleDO = newsContentArticleCoreManager
                    .buildNewsContentArticleDO(IDUtil.getNewID(), newsDO.getNewsId(), article);

            // savePicture
            save(newsDO, newsContentArticleDO, newsImageDOList);
        }
    }

    @Transactional
    void save(NewsDO newsDO, NewsContentArticleDO newsContentArticleDO,
                      List<NewsImageDO> newsImageDOList) {

        // 去重
        NewsDO dataBaseNewsDO = crawlerManager.getNewsDOByDataKey(
                newsDO.getDataKey(), new Field("newsId"));

        if (dataBaseNewsDO != null) {
            return;
        }
        // 强制未发布
        newsDO.setNewsState(NewsDO.STATE_NOT_RELEASE);
        // news
        newsMapper.insert(Update.copyWithoutNull(newsDO));
        // article
        newsContentArticleMapper.insert(Update.copyWithoutNull(newsContentArticleDO));
        // image
        if (!CollectionUtils.isEmpty(newsImageDOList)) {
            for (NewsImageDO newsImageDO : newsImageDOList) {
                newsImageMapper.insert(Update.copyWithoutNull(newsImageDO));
            }
        }
    }

    private void setCookie(Request request) {
        request.addCookie("BAIDUID", "A7C60D2C1A763B7458FA459BECB09A4B:FG=1");
        request.addCookie("BIDUPSID", "A7C60D2C1A763B7458FA459BECB09A4B");
        request.addCookie("PSTM", "1519696502");
        request.addCookie("BDORZ", "B490B5EBF6F3CD402E515D22BCDA1598");
        request.addCookie("__cfduid", "db148f82394e6677e80c1534dc07cd2901520219332");
        request.addCookie("LOCALGX", "%u5E7F%u5DDE%7C%35%34%39%36%7C%u5E7F%u5DDE%7C%35%34%39%36");
        request.addCookie("BDUSS", "RrYnhDTU8tWjJCaGxhbGVITW9xV0xUa0F2T1hWajZHdnZzTXNqflJwRG94Y2hhQVFBQUFBJCQAAAAAAAAAAAEAAADt3cQ3Q2hlcmlzaFNpbmNlAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAOg4oVroOKFaeU");
        request.addCookie("locale", "zh");
        request.addCookie("H_PS_PSSID", "1445_21126_20697");
        request.addCookie("H_WISE_SIDS", "122527_114745_103569_122643_120154_122489_118886_118862_118846_118836_118792_120549_122188_107315_121936_117334_121862_117432_121667_122296_122735_122138_120851_121143_116407_122670_110085_122303");
        request.addCookie("SE_LAUNCH", "5%3A25344222_0%3A25344222");
        request.addCookie("Hm_lvt_e9e114d958ea263de46e080563e254c4", "1520423612,1520470670,1520653383");
        request.addCookie("Hm_lpvt_e9e114d958ea263de46e080563e254c4", "1520653383");
        request.addCookie("Hm_lvt_0c8070895132126fa3ba3bb7df1ac58e", "1520326936,1520423619,1520470676,1520653387");
        request.addCookie("Hm_lpvt_0c8070895132126fa3ba3bb7df1ac58e", "1520653413");
        request.addCookie("PMS_JT", "%28%7B%22s%22%3A1520653416610%2C%22r%22%3A%22https%3A//news.baidu.com/news%22%7D%29");
    }

    private void setHeader(Request request) {
        request.addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3 like Mac OS X) AppleWebKit/602.1.50 (KHTML, like Gecko) CriOS/56.0.2924.75 Mobile/14E5239e Safari/602.1");
    }

    private String getHashTitle(String title) {
        return EncryptUtil.encrypt(title, "md5");
    }

    @Override
    public Site getSite() {
        return site;
    }
}
