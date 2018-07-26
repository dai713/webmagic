package cn.mc.scheduler.crawler.toutiao.com;

import cn.mc.core.dataObject.NewsContentVideoDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsContentVideoCoreManager;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.zip.CRC32;

/**
 * @auther sin
 * @time 2018/3/15 17:30
 */
@Component
public class VideoCrawler extends BaseCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoCrawler.class);

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static final String URL = "http://lf.snssdk.com/api/news/feed/v58/" +
            "?version_code=6.2.6&app_name=news_article&category=video&vid=3678164C-BC97-4BDE-90C3-3796BF8C39DA" +
            "&device_id=3002398707&channel=pp&resolution=750*1334&aid=13" +
            "&ab_version=157646,171199,177253,181399,172664,171194,182069,181892,178728,170354,180929,175621,1814" +
            "74,180986,174395,177587,183144,177166,152027,176590,181533,177009,31210,151777,179454,180290,181217,1" +
            "82114,170713,176742,182216,156262,145585,174429,181817,183177,181637,162572,181182,176601,176603,1766" +
            "16,170988,170989,180928,150353,176596,164095,157554,176652,177702,176606,182998&ab_feature=z2" +
            "&ab_group=z2&openudid=5f892e162435cdbae5dc2856c69bb9ecbc678040&live_sdk_version=1.6.5" +
            "&idfv=3678164C-BC97-4BDE-90C3-3796BF8C39DA&ac=WIFI&os_version=9.3.3" +
            "&ssmix=a&device_platform=iphone&iid=12374638189&ab_client=a1,f2,f7,e1&device_type=iPhone%206" +
            "&idfa=1FDA0643-15FB-4763-85F2-D073D6C7D1C5&LBS_status=authroize&city=%E6%B7%B1%E5%9C%B3" +
            "&concern_id=6215497899027991042&count=20&cp=5f9dC7C8617AAq1&detail=1&image=1&language=" +
            "zh-Hans-CN&last_refresh_sub_entrance_interval=46170&latitude=22.63033283110117&loc_mode=1" +
            "&loc_time=1506568075&longitude=114.1424558664706&min_behot_time=1506521979&refer=1" +
            "&strict=0&tt_from=enter_auto";

    private static final String VIDEO_URL_TEMPLATE =
            "http://i.snssdk.com/video/urls/v/1/toutiao/mp4/%s?r=%s&s=%s&callback=";

    private Map<String, VideoCrawlerVO> cacheData = new LinkedHashMap<>();

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsContentVideoCoreManager newsContentVideoCoreManager;
    @Autowired
    private VideoCrawlerPipeline videoCrawlerPipeline;

    @Override
    public Spider createCrawler() {
        BaseSpider spider = new BaseSpider(this);
        spider.addUrl(URL);
        return spider;
    }

    @Override
    public synchronized void process(Page page) {
        if (URL.equals(page.getUrl().toString())) {
            // handle list
            handleVideoList(page);
        } else {
            // handle video info
            handleVideoInfo(page);
        }
    }

    private void handleVideoInfo(Page page) {
        JSONObject jsonObject = JSONObject.parseObject(page.getRawText().toString());

        String url = page.getUrl().toString();

        JSONObject dataJsonObject = (JSONObject) jsonObject.get("data");
        String videoId = String.valueOf(dataJsonObject.get("video_id"));
        String videoImage = String.valueOf(dataJsonObject.get("poster_url"));

        Long videoDuration;
        try {
            Double d1 = Double.valueOf(String.valueOf(dataJsonObject.get("video_duration")));
            videoDuration = Math.round(d1);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Error: {}", ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("视频时间转换失败 自动过滤");
        }

        JSONObject videoListJsonObject= (JSONObject) dataJsonObject.get("video_list");
        JSONObject chooseVideoJsonObject = chooseVideo(videoListJsonObject);

        String fileUrl = base64Decode(String.valueOf(chooseVideoJsonObject.get("main_url")));
        if (fileUrl == null)
            return;

        Integer videoWidth = Integer.valueOf(chooseVideoJsonObject.get("vwidth").toString());
        Integer videoHeight = Integer.valueOf(chooseVideoJsonObject.get("vheight").toString());
        Integer videoSize = Integer.valueOf(chooseVideoJsonObject.get("size").toString());

        VideoCrawlerVO videoCrawlerVO = cacheData.get(videoId);
        NewsDO newsDO = videoCrawlerVO.getNewsDO();
        String videoUrl = newsDO.getNewsSourceUrl();

        String videoIntroduce = newsDO.getNewsAbstract();
        NewsContentVideoDO newsContentVideoDO
                = newsContentVideoCoreManager.buildNewsContentVideoDO(
                IDUtil.getNewID(), newsDO.getNewsId(), videoId, videoUrl,
                fileUrl, videoDuration, NewsContentVideoDO.FORMAT_MP4,
                videoIntroduce, videoImage, videoWidth, videoHeight,
                NewsContentVideoDO.AUTH_ACCESS_YES, videoSize);

        // 保存
        videoCrawlerPipeline.saveVideo(newsDO, newsContentVideoDO, videoCrawlerVO);
    }


    public String base64Decode(String base64Url) {
        try {
            return new String(Base64.getDecoder().decode(base64Url), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Error: {}", ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    private Integer getVideoFormat(JSONObject chooseVideoJsonObject) {
        String videoType = String.valueOf(chooseVideoJsonObject.get("vtype"));
        if (videoType.equals("mp4")) {
            return NewsContentVideoDO.FORMAT_MP4;
        } else {
            return NewsContentVideoDO.FORMAT_NON;
        }
    }

    private JSONObject chooseVideo(JSONObject videoListJsonObject) {
        String videoKey = "video_%s";
        int videoSize = videoListJsonObject.size();
        if (videoSize >= 3 || videoSize == 2) {
            return (JSONObject) videoListJsonObject.get(String.format(videoKey, 2));
        } else {
            return (JSONObject) videoListJsonObject.get(String.format(videoKey, 1));
        }
    }

    private void handleVideoList(Page page) {
        JSONObject resultDataJsonObject = JSON.parseObject(page.getRawText());
        JSONArray dataJsonArray = (JSONArray) resultDataJsonObject.get("data");
        List<JSONObject> contentJsonObjectList = getContentList(dataJsonArray);
        if (CollectionUtils.isEmpty(contentJsonObjectList))
            return;

        for (Object dataObject : dataJsonArray) {
            JSONObject dataJsonObject = (JSONObject) dataObject;

            String contentJsonString = String.valueOf(dataJsonObject.get("content"));
            if (StringUtils.isEmpty(contentJsonString))
                return;

            // content object
            JSONObject contentJsonObject = JSONObject.parseObject(contentJsonString);
            Long newsId = IDUtil.getNewID();
            String videoId = String.valueOf(contentJsonObject.get("video_id"));
            String title= String.valueOf(contentJsonObject.get("title"));
            String newsAbstract= String.valueOf(contentJsonObject.get("abstract"));
            String newsSource = "西瓜视频";
            String newsSourceUrl = String.valueOf(contentJsonObject.get("display_url"));
            Date displayTime = DateUtil.currentDate();

            Date createTime = displayTime;
            String dataKey = videoId;
            int videoCount = 1;
            int displayType = handleVideoDisplayType();

            // 视频的图片 (包含大小图)
            List<NewsImageDO> newsImageDOList = getNewsImageDO(contentJsonObject, newsId);
            Integer sourceCommentCount=Integer.valueOf(contentJsonObject.get("comment_count").toString());
            int imageCount = newsImageDOList.size();
            Integer commentCount = 0;
            NewsDO newsDO = newsCoreManager.buildNewsDO(newsId, dataKey, title,
                    0,
                    "", "", newsSource, newsSourceUrl,
                    NewsDO.NEWS_TYPE_VIDEO, NewsDO.CONTENT_TYPE_VIDEO,
                    "", 0, newsAbstract, displayTime,
                    videoCount, imageCount, createTime, displayType,sourceCommentCount,
                    commentCount);

            // cache data
            VideoCrawlerVO videoCrawlerVO = new VideoCrawlerVO();
            videoCrawlerVO.setNewsDO(newsDO);
            videoCrawlerVO.setNewsImageDOList(newsImageDOList);
            cacheData.put(dataKey, videoCrawlerVO);
        }

        List<String> videoInfoUrlList = getVideoInfoUrl(contentJsonObjectList);
        page.addTargetRequests(videoInfoUrlList);
    }

    private List<NewsImageDO> getNewsImageDO(JSONObject contentJsonObject, Long newsId) {
        // large image
        JSONArray jsonArray = (JSONArray) contentJsonObject.get("large_image_list");
        List<NewsImageDO> largeNewsImageDOList = new ArrayList<>(jsonArray.size());
        for (Object largeImageObject : jsonArray) {
            JSONObject largeImageJsonObject = (JSONObject) largeImageObject;
            largeNewsImageDOList.add(getNewsImageDO(
                    largeImageJsonObject, newsId, NewsImageDO.IMAGE_TYPE_LARGE));
        }

        // middle image
        JSONObject middleImageJsonObject = (JSONObject) contentJsonObject.get("middle_image");
        if (middleImageJsonObject != null) {
            NewsImageDO miniNewsImageDO = getNewsImageDO(
                    middleImageJsonObject, newsId, NewsImageDO.IMAGE_TYPE_MINI);

            largeNewsImageDOList.add(miniNewsImageDO);
        }

        // return all
        return largeNewsImageDOList;
    }

    private NewsImageDO getNewsImageDO(JSONObject imageJsonObject, Long newsId, Integer imageType) {
        Object heightObject = imageJsonObject.get("height");
        Object widthObject = imageJsonObject.get("width");
        if (heightObject == null
                || widthObject == null) {
            return  null;
        }

        Integer imageHeight = Integer.valueOf(String.valueOf(heightObject));
        Integer imageWidth = Integer.valueOf(String.valueOf(widthObject));
        String imageUrl = String.valueOf(imageJsonObject.get("url"));
        return newsImageCoreManager.buildNewsImageDO(IDUtil.getNewID(),
                newsId, imageUrl, imageWidth, imageHeight,
                imageType);
    }

    private List<String> getVideoInfoUrl(List<JSONObject> contentJsonObjectList) {
        List<String> videoInfoUrls = new ArrayList<>(contentJsonObjectList.size());
        for (Object object : contentJsonObjectList) {
            JSONObject contentJsonObject = (JSONObject) object;
            String videoId = String.valueOf(contentJsonObject.get("video_id"));
            videoInfoUrls.add(buildCrc32Url(videoId));
        }
        return videoInfoUrls;
    }

    private String buildCrc32Url(String videoId) {
        int random = (int) ((int)1000000000 + (Math.random() * 1000000000));
        String crc32Text = String.format("/video/urls/v/1/toutiao/mp4/%s?r=%s", videoId, random);
        CRC32 crc32 = new CRC32();
        crc32.update(crc32Text.getBytes());
        Long crc32Value = crc32.getValue();
        return String.format(VIDEO_URL_TEMPLATE, videoId, random, crc32Value);
    }

    private List<JSONObject> getContentList(JSONArray dataJsonArray) {
        List<JSONObject> contentJsonObjectList = new ArrayList<>();
        for (Object dataObject : dataJsonArray) {
            JSONObject dataJsonObject = (JSONObject) dataObject;
            // content
            contentJsonObjectList.add(
                    JSON.parseObject(
                            String.valueOf(dataJsonObject.get("content"))));
        }
        return contentJsonObjectList;
    }

    @Override
    public Site getSite() {
        return site;
    }
}
