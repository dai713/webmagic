package cn.mc.scheduler.crawler.toutiao.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsContentArticleCoreManager;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.IDUtil;
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
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

import java.util.*;

/**
 *
 * @auther sin
 * @time 2018/3/8 15:42
 */
@Component
@Deprecated
public class EntertainmentCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(2000);

    private static final String URL = "http://m.toutiao.com/list/?tag=news_entertainment&ac=wap" +
            "&count=20&format=json_raw&as=A1A5FA6A20DE8E3&cp=5AA05E48BE531E1&max_behot_time=%s";

    private static Map<String, NewsDO> cacheNewsDO = Collections.EMPTY_MAP;
    private static Map<String, List<NewsImageDO>> cacheNewsImageDO = Collections.EMPTY_MAP;

    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsImageMapper newsImageMapper;
    @Autowired
    private NewsContentArticleMapper newsContentArticleMapper;
    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsContentArticleCoreManager newsContentArticleCoreManager;
    @Autowired
    private CrawlerManager crawlerManager;

    private static List<String> REQUEST_URLS = Collections.EMPTY_LIST;

    @Override
    public Spider createCrawler() {
        // init request
        int requestSize = 5;
        REQUEST_URLS = new ArrayList<>(requestSize);
        List<Request> requests = new ArrayList<>();
        for (int i = 0; i < requestSize; i++) {
            String url = String.format(URL, System.currentTimeMillis() / 1000 - 2000 * i, i);
            REQUEST_URLS.add(url);

            Request request = new Request(url);
            request.addHeader(HEADER_USER_AGENT_KEY, USER_AGENT_IPHONE_OS);
            request.addCookie("UM_distinctid", "161faada67a60e-08f277d7d3943b-32667b04-13c680-161faada67bae0");
            request.addCookie("tt_webid", "6529763932625012232");
            request.addCookie("csrftoken", "3d438c69f5b8d01de2e5a258cbbdc0a0");
            request.addCookie("W2atIF", "1");
            request.addCookie("_ba", "BA0.2-20180306-51225-ZUjP1b5o41jIXiQ2ttPe");
            requests.add(request);
        }
        return Spider.create(this)
                .addRequest(requests.toArray(new Request[requestSize]));
    }

    @Override
    public void process(Page page) {

        if (page.getRequest().getUrl().matches("http://m.toutiao.com.*")) {

            JSONObject jsonObject = JSON.parseObject(page.getRawText());

            // 必须请求成功
            if (!(Boolean)jsonObject.get("has_more")) {
                return;
            }

            JSONArray dataJSONOArray = (JSONArray) jsonObject.get("data");
            cacheNewsDO = new LinkedHashMap<>(dataJSONOArray.size());
            cacheNewsImageDO = new LinkedHashMap<>();
            for (Object object : dataJSONOArray) {
                JSONObject jsonDataObject = (JSONObject) object;
                String dataKey = String.valueOf(jsonDataObject.get("item_id"));
                String title = String.valueOf(jsonDataObject.get("title")).trim();
                String source = String.valueOf(jsonDataObject.get("source"));
                String newsSourceUrl = String.valueOf(jsonDataObject.get("article_url"));
                String keywords = String.valueOf(jsonDataObject.get("keywords"));

                if (keywords.length() >= 50) {
                    keywords = keywords.substring(0, keywords.substring(0, 50).lastIndexOf(","));
                }
                Integer banComment = Integer.valueOf(jsonDataObject.get("ban_comment").toString());
                String newsAbstract = String.valueOf(jsonDataObject.get("abstract")).trim();
                Integer displayDataTime = Integer.valueOf(jsonDataObject.get("display_dt").toString());
                Integer hasVideo = Integer.valueOf(jsonDataObject.get("has_mp4_video").toString());

                Date createTime = new Date(Integer.valueOf(
                        jsonDataObject.get("publish_time").toString()) * 1000);

                JSONArray jsonArray = (JSONArray) jsonDataObject.get("image_list");
                Integer imageCount = jsonArray.size();

                String newsUrl = "";
                String shareUrl = "";
                Integer newsHot = 0;
                Integer videoCount = hasVideo;
                Long newsId = IDUtil.getNewID();
                Date displayTime = new Date(displayDataTime * 1000);
                Integer displayType = handleNewsDisplayType(imageCount);
                NewsDO newsDO = newsCoreManager.buildNewsDO(
                        newsId, dataKey, title, newsHot,
                        newsUrl, shareUrl, source, newsSourceUrl,
                        NewsDO.NEWS_TYPE_ENTERTAINMENT,
                        NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                        keywords, banComment,
                        newsAbstract, displayTime,
                        videoCount, imageCount, createTime,
                        displayType,0, 0);

                List<NewsImageDO> newsImageDOList = new ArrayList<>(imageCount);
                if (!CollectionUtils.isEmpty(jsonArray)) {
                    for (Object imageObject : jsonArray) {
                        JSONObject jsonImageObject = (JSONObject) imageObject;
                        String imageUrl = String.valueOf(jsonImageObject.get("url"));
                        Integer imageWidth = Integer.valueOf(jsonImageObject.get("width").toString());
                        Integer imageHeight = Integer.valueOf(jsonImageObject.get("height").toString());

                        newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                                IDUtil.getNewID(), newsId,
                                imageUrl, imageWidth, imageHeight,
                                NewsImageDO.IMAGE_TYPE_MINI));
                    }
                }
                newsDO.setImageCount(imageCount);

                // cache
                cacheNewsDO.put(newsDO.getDataKey(), newsDO);
                cacheNewsImageDO.put(newsDO.getDataKey(), newsImageDOList);

                // 继续抓取 详情页面
                String url = String.format("https://toutiao.com/i%s", dataKey);
                page.addTargetRequests(Arrays.asList(url));
            }
        } else if (page.getRawText().indexOf("<!DOCTYPE html>") != -1) {

            // 处理内容
            // 内容数据在 <script> 标签中，且不是一个标准的 JSON 格式
            // 匹配中不要替换 \n 特殊字符，匹配玩后刚好一行得到 想要的 content 内容
            String scriptJsonData = page.getHtml()
                    .xpath("//script")
                    .regex("BASE_DATA =.*")
                    .toString()
                    .replaceFirst("BASE_DATA =", "")
                    .replaceAll("</script>", "")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">")
                    .replaceAll("&#x3D;", "=")
                    .replaceAll("&quot;", "\"")
                    .replaceAll("&quot;", "\"");

            // 活动内容 line
            String articleLine = findRegex(scriptJsonData, "content(.+)");

            // 活动内容 value
            String articleContent =
                    findRegex(articleLine, "'.+'").replaceAll("'", "");

            // 获取 dataKey
            String dataKey = page.getUrl().regex("/i.*").toString().replaceAll("/i", "");

            if (StringUtils.isEmpty(articleContent)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }

            if (!saveNews(dataKey, articleContent)) {
                return;
            }
        }
    }

    @Transactional
    public boolean saveNews(String dataKey, String articleContent) {

        // 获取新闻
        NewsDO newsDO = cacheNewsDO.get(dataKey);

        NewsDO dataBaseNewsDO = crawlerManager.listNewsDOByDataKey(
                newsDO.getDataKey(), new Field("newsId"));

        if (dataBaseNewsDO != null) {
            return false;
        }

        // build 一个文章类型
        NewsContentArticleDO contentArticleDO = newsContentArticleCoreManager
                .buildNewsContentArticleDO(newsDO.getNewsId(), IDUtil.getNewID(), articleContent);
        newsContentArticleMapper.insert(Update.copyWithoutNull(contentArticleDO));
        newsMapper.insert(Update.copyWithoutNull(newsDO));

        // 新闻 image
        if (cacheNewsImageDO.containsKey(dataKey)) {
            List<NewsImageDO> newsImageDOList = cacheNewsImageDO.get(dataKey);
            for (NewsImageDO newsImageDO : newsImageDOList) {
                newsImageMapper.insert(Update.copyWithoutNull(newsImageDO));
            }
        }
        return true;
    }

    @Override
    public Site getSite() {
        return site;
    }
}
