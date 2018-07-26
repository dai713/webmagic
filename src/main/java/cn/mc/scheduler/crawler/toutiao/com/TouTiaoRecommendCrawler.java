package cn.mc.scheduler.crawler.toutiao.com;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;

import java.util.*;

/**
 * @author sin
 * @time 2018/7/20 14:51
 */
@Component
@Deprecated
public class TouTiaoRecommendCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(300);

    // max_behot_time=1532066832&i=1532066832&
    private static final String URL = "https://m.toutiao.com/list/?tag=__all__&ac=wap&count=20" +
            "&format=json_raw&as=A1754B9555336B6&cp=5B5543F65BB69E1" +
            "&min_behot_time=1532311219&_signature=1CbacgAAj3xrUCuUtzr0.dQm2m&i=1532284967";

    private static Map<String, NewsDO> cacheNewsDO = Collections.EMPTY_MAP;
    private static Map<String, List<NewsImageDO>> cacheNewsImageDO = Collections.EMPTY_MAP;

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;

    private static List<String> REQUEST_URLS = Collections.EMPTY_LIST;

    @Autowired
    private EntertainmentCrawler2Pipeline entertainmentCrawler2Pipeline;

    @Override
    public Spider createCrawler() {

        // init cache
        cacheNewsDO = new LinkedHashMap<>();
        cacheNewsImageDO = new LinkedHashMap<>();

        // init request
        int requestSize = 1;
        REQUEST_URLS = new ArrayList<>(requestSize);
        List<Request> requests = new ArrayList<>();

        for (int i = 0; i < requestSize; i++) {

            long minBeHostTime = System.currentTimeMillis() / 1000 - + 1200 + new Random().nextInt(10000);
            long iTime = System.currentTimeMillis() / 1000 - 1200 + new Random().nextInt(10000);

            // max_behot_time=1532066832&i=1532066832&
            String url = String.format("%s&max_behot_time=%s&i=%s", URL, minBeHostTime, iTime);
            REQUEST_URLS.add(url);
            Request request = new Request(url);
            addHeaders(request);
            requests.add(request);
        }

        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(requests.toArray(new Request[requestSize]));
        return spider;
    }


    @Override
    public Site getSite() {
        return site;
    }

    @Override
    public void process(Page page) {

        if (REQUEST_URLS.contains(page.getUrl().toString())) {

            JSONObject jsonObject = JSON.parseObject(page.getRawText());
            JSONArray dataJSONOArray = (JSONArray) jsonObject.get("data");

            for (Object object : dataJSONOArray) {
                JSONObject jsonDataObject = (JSONObject) object;

                Object adLabel = jsonDataObject.get("ad_label");

                if (adLabel != null) {
                    if (String.valueOf(adLabel).equals( "广告")) {
                        continue;
                    }
                }

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
                Long displayDataTime = Long.valueOf(jsonDataObject.get("display_dt").toString()) * 1000;
                Integer hasVideo = Integer.valueOf(jsonDataObject.get("has_mp4_video").toString());

                Date createTime = new Date(Integer.valueOf(
                        jsonDataObject.get("publish_time").toString()) * 1000);

                JSONArray jsonArray = (JSONArray) jsonDataObject.get("image_list");
                Integer imageCount = jsonArray.size();
                Integer sourceCommentCount = Integer.valueOf(jsonDataObject.get("comment_count").toString());

                String newsUrl = "";
                String shareUrl = "";
                Integer newsHot = 0;
                Integer videoCount = hasVideo;
                Long newsId = IDUtil.getNewID();
                Date displayTime = new Date(displayDataTime);
                Integer displayType = handleNewsDisplayType(imageCount);
                NewsDO newsDO = newsCoreManager.buildNewsDO(
                        newsId, dataKey, title, newsHot,
                        newsUrl, shareUrl, source, newsSourceUrl,
                        NewsDO.NEWS_TYPE_ENTERTAINMENT,
                        NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                        keywords, banComment,
                        newsAbstract, displayTime,
                        videoCount, imageCount, createTime,
                        displayType, sourceCommentCount, 0);

                // 获取 image_list
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

                // todo newsImage 等于 empty 直接return，暂时这样处理，这样 return 会缺少 “纯文字” 的新闻
                if (CollectionUtils.isEmpty(newsImageDOList)) {
                    continue;
                }

                newsDO.setImageCount(imageCount);

                // cache
                cacheNewsDO.put(dataKey, newsDO);
                cacheNewsImageDO.put(dataKey, newsImageDOList);

                // 继续抓取 详情页面
                // https://m.toutiao.com/i6535233703872823821/info/?_signature=213423412-Tp-J0Ss35UE4bn&i=6535233703872823821
                String url = String.format("http://m.toutiao.com/i%s/info/" +
                        "?_signature=213423412-Tp-J0Ss35UE4bn&i=%s&dataKey=%s", dataKey, dataKey, dataKey);

                Request request = new Request(url);
                addHeaders(request);
                page.addTargetRequest(request);
            }
        } else {

            JSONObject infoJsonObject = JSONObject.parseObject(page.getRawText());
            JSONObject dataJsonObject = (JSONObject) infoJsonObject.get("data");
            String url = String.valueOf(dataJsonObject.get("url"));
            String title = String.valueOf(dataJsonObject.get("title"));
            String content = String.valueOf(dataJsonObject.get("content"));
            String publishTime = String.valueOf(dataJsonObject.get("publish_time"));

            // 获取 dataKey
            Map<String, String> urlParams = analyticalUrlParams(page.getUrl().toString());

            if (!urlParams.containsKey("dataKey")) {
                return;
            }

            // 视频 内容先过滤掉
            if (Html.create(content).xpath("//[@tt-videoid]").nodes().size() > 0) {
                return;
            }

            String dataKey = urlParams.get("dataKey");
            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }

            // 去保存
            entertainmentCrawler2Pipeline.saveEntertainment(
                    dataKey, cacheNewsDO, cacheNewsImageDO, content);
        }
    }

    private void addHeaders(Request request) {
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        request.addHeader("Accept-Encoding", "gzip, deflate, br");
        request.addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        request.addHeader("Cache-Control", "no-cache");
        request.addHeader("Connection", "keep-alive");
//        request.addHeader("Cookie", "UM_distinctid=161faada67a60e-08f277d7d3943b-32667b04-13c680-161faada67bae0; csrftoken=3d438c69f5b8d01de2e5a258cbbdc0a0; _ba=BA0.2-20180306-51225-ZUjP1b5o41jIXiQ2ttPe; uuid=\"w:371f621cb04b46f0b99bcd8244b2c65b\"; _ga=GA1.2.974145270.1520908125; __utma=255578007.974145270.1520908125.1521717409.1521717409.1; __utmz=255578007.1521717409.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); tt_webid=6569453223143474702; Hm_lvt_23e756494636a870d09e32c92e64fdd6=1530685619,1531452184,1531718804");
        request.addHeader("Host", "m.toutiao.com");
        request.addHeader("Pragma", "no-cache");
        request.addHeader("Upgrade-Insecure-Requests", "1");
        request.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
    }
}
