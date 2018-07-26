package cn.mc.scheduler.crawler.wangyi.com;


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
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * 网易视频
 * @author daiqingwen
 * @date 2018-6-8 下午16:35
 */
@Component
public class WangyiVideoCrawlers extends BaseCrawler {

    private static final Logger logger = LoggerFactory.getLogger(WangyiVideoCrawlers.class);

    private Site site = Site.me().setRetrySleepTime(3).setSleepTime(1000);

    @Autowired
    private NewsContentVideoCoreManager videoCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private WangyiVideoPipeline wangyiVideoPipeline;

    @Override
    public Spider createCrawler() {
        String VIDEO_URL = "https://c.m.163.com/recommend/getChanListNews?channel=T1457068979049" +
                "&passport=&devId=IIuuXb/hLUSPTntXq4kI%2BoRnb0c8Tfr%2BE7bHyJYx5KpjHUvhlNSkVu2N48qn/bQm" +
                "&version=37.1&spever=false&net=wifi&lat=&lon=&ts=1528438782&sign=zREo9KopFt13hDOFqafmK/2k" +
                "FOAo4Ae06gt1Dt6iGkt48ErR02zJ6/KXOnxX046I&encryption=1&canal=appstore&offset=0&size=10&fn=9";

        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(new Request().setUrl(VIDEO_URL));
        return spider;
    }

    @Override
    public synchronized void process(Page page) {
        logger.info("开始抓取网易视频...");

        try {

            String pageUrl = String.valueOf(page.getUrl());
            JSONObject jsonObject = JSON.parseObject(page.getRawText());
            JSONArray data = jsonObject.getJSONArray("视频");
            Map<Long, NewsContentVideoDO> videoMap = Maps.newHashMap();
            Map<Long, NewsDO> newsMap = Maps.newHashMap();
            Map<Long, NewsImageDO> newsImageMap = Maps.newHashMap();
            for (int i = 0; i < data.size(); i++) {
                Long newsId = IDUtil.getNewID();
                Long newsContentVideoId = IDUtil.getNewID();

                JSONObject obj = data.getJSONObject(i);
                String dataKey = this.encrypt(String.valueOf(IDUtil.getNewID()));
                URL url = new URL(obj.getString("mp4_url"));
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                con.connect();
                int videoSize = con.getContentLength();

                // 上传图片至阿里云
                String videoImage = aliyunOSSClientUtil.uploadPicture(obj.getString("cover"));

                Date displayTime = DateUtil.currentDate();
                Integer sourceCommentCount = obj.getInteger("voteCount");
                Integer commentCount = 0;
                String title = obj.getString("title");
                int newsHot = 0;
                String newsUrl = "";
                String shareUrl = "";

                String newsSource = obj.getString("videosource");
                String newsSourceUrl = pageUrl;

                int newsType = NewsDO.NEWS_TYPE_VIDEO;
                int contentType = NewsDO.CONTENT_TYPE_VIDEO;

                String keywordsString = obj.getString("unlikeReason");
                String keywords = keywordsString.replaceAll("\\[", "")
                        .replaceAll("]", "")
                        .replaceAll("\"", "")
                        .replaceAll("/\\d+", "");

                JSONArray extraTags = obj.getJSONArray("extraTags");
                StringBuffer stringBuffer = new StringBuffer();
                for (Object object : extraTags) {
                    String tag = String.valueOf(object);
                    stringBuffer.append(",");
                    stringBuffer.append(tag);
                }

                // 添加 extraTags
                keywords += stringBuffer.toString();

                int banComment = 0;
                String newsAbstract = obj.getString("topicDesc");
                int videoCount = 1;
                int imageCount = 1;

                Date createTime = DateUtil.currentDate();
                int displayType = NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE;

                NewsDO newsDO =  newsCoreManager.buildNewsDO(newsId, dataKey, title,
                        newsHot, newsUrl, shareUrl, newsSource, newsSourceUrl,
                        newsType, contentType, keywords, banComment, newsAbstract,
                        displayTime, videoCount, imageCount, createTime, displayType,
                        sourceCommentCount, commentCount);

                // 封装视频对象
                String videoUrl = "";
                String fileUrl = obj.getString("mp4_url");
                long durationTime = obj.getLong("length");
                String videoIntroduce = title;

                int videoFormat = NewsContentVideoDO.FORMAT_MP4;
                int videoImageWidth = -1;
                int videoImageHeight = -1;

                int authAccess = NewsContentVideoDO.AUTH_ACCESS_NO;
                NewsContentVideoDO videoDO = videoCoreManager.buildNewsContentVideoDO(
                        newsContentVideoId, newsId, dataKey, videoUrl, fileUrl, durationTime,
                        videoFormat, videoIntroduce, videoImage, videoImageWidth,
                        videoImageHeight, authAccess, videoSize);


                // news image

                NewsImageDO newsImageDO = newsImageCoreManager.buildNewsImageDO(
                        IDUtil.getNewID(), newsId, videoImage, videoImageWidth,
                        videoImageHeight, NewsImageDO.IMAGE_TYPE_LARGE);

                newsMap.put(newsId, newsDO);
                newsImageMap.put(newsId, newsImageDO);
                videoMap.put(newsId, videoDO);
            }
            wangyiVideoPipeline.save(newsMap, newsImageMap, videoMap);
        } catch (Exception e) {
            logger.error("抓取网易视频失败{}：", e);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}