package cn.mc.scheduler.crawler.news.baidu.com;

import cn.mc.core.constants.CodeConstants;
import cn.mc.core.dataObject.NewsContentVideoDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsContentVideoCoreManager;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.Http2Util;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.util.CrawlerUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 百度 - 体育视频
 *
 * todo 暂不使用
 *
 * @author sin
 * @time 2018/7/23 19:22
 */
@Component
@Deprecated
public class BaiDuSportsVideoCrawler extends BaseCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaiDuSportsVideoCrawler.class);

    private Site site = Site.me().setRetryTimes(3).setSleepTime(200);

    private static final String URL = "https://tiyu.baidu.com/api/video/" +
            "tab/%E6%8E%A8%E8%8D%90/pagenum";

    private static final String DETAIL_URL_TEMPLATE = "http://tiyu.baidu.com/%s";

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsContentVideoCoreManager newsContentVideoCoreManager;
    @Autowired
    private BaiDuSportsVideoPipeline baiDuSportsVideoPipeline;

    private List<String> cacheUrl = Lists.newArrayList();
    private Map<String, NewsDO> cacheNewsDO = Maps.newHashMap();
    private Map<String, NewsContentVideoDO> cacheNewsContentVideoDO = Maps.newHashMap();
    private Map<String, List<NewsImageDO>> cacheNewsImageDO = Maps.newHashMap();

    @Override
    public Spider createCrawler() {

        // 构建 request url
        int size = 1;
        List<Request> requests = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {

            // 构建 url
            String url = String.format("%s/%s/pagecount/20", URL, size + 1);
            cacheUrl.add(url);

            // 构建 request
            Request request = new Request(url);
            requests.add(request);
        }

        // 添加 request
        Request request = new Request();
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(requests.toArray(new Request[]{}));
        return spider;
    }

    @Override
    public synchronized void process(Page page) {
        String url = page.getUrl().toString();
        if (cacheUrl.contains(url)) {
            listPage(page);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    private void listPage(Page page) {
        String resultJSONText = page.getRawText();
        JSONObject resultJSONObject = JSON.parseObject(resultJSONText);

        String status = resultJSONObject.getString("status");
        if (!status.equals(String.valueOf(CodeConstants.SUCCESS))) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("百度体育列表返回 status 不对! status {}", status);
            }
        }

        JSONObject dataJSONObject = resultJSONObject.getJSONObject("data");
        JSONArray listJSONArray = dataJSONObject.getJSONArray("list");

        for (Object itemObject : listJSONArray) {
            JSONObject itemJSONObject = (JSONObject) itemObject;

            String title = itemJSONObject.getString("title");
            String videoFile = itemJSONObject.getString("videoUrl");

            String newsSource = itemJSONObject.getString("source");
            String like = itemJSONObject.getString("link");
            String newsSourceUrl = String.format(DETAIL_URL_TEMPLATE, like);

            // 处理 持续时间
            String durationText = itemJSONObject.getString("duration");
            String[] durationArray = durationText.split(":");
            Integer minute = Integer.valueOf(durationArray[0]);
            Integer second = Integer.valueOf(durationArray[1]);
            long durationTime = minute * 60 + second;

            // 新闻 id 和 dataKey
            long newsId = IDUtil.getNewID();
            String dataKey = CrawlerUtil.getDataKey(newsSourceUrl);

            // 处理 newsImage
            String imgUrl = itemJSONObject.getString("img");
            Map<String, String> imageUrlParams = analyticalUrlParams(imgUrl);

            List<NewsImageDO> newsImageDOList = Lists.newArrayList();
            if (imageUrlParams.containsKey("size")) {
                String[] imageSizes = imageUrlParams.get("size")
                        .replaceAll("f", "")
                        .split("_");

                Integer imageWidth = Integer.valueOf(imageSizes[0]);
                Integer imageHeight = Integer.valueOf(imageSizes[1]);

                NewsImageDO newsImageDO = newsImageCoreManager.buildNewsImageDO(
                        IDUtil.getNewID(), newsId, imgUrl, imageWidth,
                        imageHeight, NewsImageDO.IMAGE_TYPE_LARGE);

                newsImageDOList.add(newsImageDO);
            }

            // 处理 news
            int newsHot = 0;
            String newsUrl = "";
            String shareUrl = "";
            int newsType = NewsDO.NEWS_TYPE_SPORTS;
            int contentType = NewsDO.CONTENT_TYPE_VIDEO;
            int banComment = 0;
            Date displayTime = cn.mc.core.utils.DateUtil.currentDate();
            int videoCount = 1;
            int imageCount = newsImageDOList.size();
            int displayType = handleNewsDisplayType(imageCount);
            int commentCount = 0;
            int sourceCommentCount = 0;
            String keywords = "";
            String newsAbstract = "";
            Date createTime = DateUtil.currentDate();

            NewsDO newsDO = newsCoreManager.buildNewsDO(newsId, dataKey, title, newsHot,
                    newsUrl, shareUrl, newsSource, newsSourceUrl, newsType,
                    contentType, keywords, banComment, newsAbstract, displayTime,
                    videoCount, imageCount, createTime, displayType,
                    sourceCommentCount, commentCount);

            // 处理 newsContentVideo

            String videoDataUrl = "http://h5vv.video.qq.com/getinfo?callback=a" +
                    "&platform=11001&charge=0&otype=json&ehost=http%3A%2F%2Fv.qq.com" +
                    "&sphls=0&sb=1&nocache=0&_rnd=1532422443&guid=df7a8e1179dd07f51736a1ff1df94" +
                    "cc0&appVer=V2.0Build9496&defaultfmt=auto&&_qv_rmt=y5cFhgbBA1" +
                    "3424u5k=&_qv_rmt2=P38jYink152243Z1w=&sdtfrom=v3010&callback=tvp_request_getin" +
                    "fo_callback_964932";

            Map<String, String> videoParams = analyticalUrlParams(videoFile);
            if (!videoParams.containsKey("vid")) {
                return;
            }

            String vid = videoParams.get("vid");
            String videoRequestUrl = String.format("%s&vids=%s", videoDataUrl, vid);
            String resultJSONP = Http2Util.httpGet(videoRequestUrl, null);
            String resultJSON = resultJSONP.substring(2, resultJSONP.length() - 1);

            JSONObject videoJSONObject = JSON.parseObject(resultJSON);
            JSONObject vlJSONObject = videoJSONObject.getJSONObject("vl");
            JSONArray viJSONArray = vlJSONObject.getJSONArray("vi");
            for (Object viObject : viJSONArray) {
                JSONObject viJSONObject = (JSONObject) viObject;
                String fileVideoKey = viJSONObject.getString("fvkey");
                String fileName = viJSONObject.getString("fn");

                JSONObject ulJSONObject = viJSONObject.getJSONObject("ul");
                JSONArray uiJSONArray = ulJSONObject.getJSONArray("ui");
                JSONObject uiJSONObject = (JSONObject) uiJSONArray.get(0);
                String urlString = uiJSONObject.getString("url");

                String authVideoFile = String.format("%s/%s?vkey=%s&br=121" +
                        "&platform=2&fmt=auto&level=0&sdtfrom=v3010" +
                        "&guid=df7a8e1179dd07f51736a1ff1df94cc0",
                        urlString, fileName, fileVideoKey);

                videoFile = authVideoFile;
            }


            String videoUrl = "";
            String videoIntroduce = title;

            NewsImageDO newsImageDO = newsImageDOList.get(0);

            String videoImageUrl = newsImageDO.getImageUrl();
            Integer videoImageWidth = newsImageDO.getImageWidth();
            Integer videoImageHeight = newsImageDO.getImageHeight();
            Integer videoSize = CrawlerUtil.getVideoSize(videoFile);
            if (videoSize == null) {
                videoSize = 0;
            }

            NewsContentVideoDO newsContentVideoDO =
                    newsContentVideoCoreManager.buildNewsContentVideoDO(
                    IDUtil.getNewID(), newsId, dataKey, videoUrl,
                    videoFile, durationTime, NewsContentVideoDO.FORMAT_MP4,
                    videoIntroduce, videoImageUrl, videoImageWidth, videoImageHeight,
                    NewsContentVideoDO.AUTH_ACCESS_NO, videoSize);

            // 添加到 cache
            cacheNewsDO.put(dataKey, newsDO);
            cacheNewsImageDO.put(dataKey, newsImageDOList);
            cacheNewsContentVideoDO.put(dataKey, newsContentVideoDO);

            // 去保存
            baiDuSportsVideoPipeline.save(dataKey, cacheNewsDO,
                    cacheNewsContentVideoDO, cacheNewsImageDO);
        }
    }
}
