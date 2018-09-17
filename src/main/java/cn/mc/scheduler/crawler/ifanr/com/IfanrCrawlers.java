package cn.mc.scheduler.crawler.ifanr.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsContentArticleCoreManager;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.CollectionUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 爱范儿网新闻
 *
 * @author daiqingwen
 * @date 2018/7/24 下午 17:45
 */
@Component
public class IfanrCrawlers extends BaseCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IfanrCrawlers.class);

    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(300);

    private static final String url = "https://sso.ifanr.com/api/v5/wp/feed/?limit=12&offset=0";

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsContentArticleCoreManager newsContentArticleCoreManager;
    @Autowired
    private SchedulerUtils schedulerUtils;
    @Autowired
    private IfanrPipeline ifanrPipeline;

    /**
     * 存储爬虫抓取的 position
     *
     *  如果没有信息更新则，不抓取内容
     */
    private static Map<String, String> CACHE_POSITION = Maps.newHashMap();
    /**
     * cache position
     */
    private static final String CACHE_POSITION_KEY = "CACHE_POSITION";

    @Override
    public Spider createCrawler() {
        Request request = new Request();
        request.setUrl(url);
        return BaseSpider.create(this).addRequest(request);
    }

    @Override
    public void process(Page page) {
        getList(page);
    }

    @Override
    public Site getSite() {
        return SITE;
    }

    /**
     * 获取新闻列表
     *
     * @param page
     */
    private void getList(Page page) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("开始抓取爱范儿网新闻...");
        }

        JSONObject jsonObject = JSON.parseObject(page.getRawText());
        JSONArray objectsJSONArray = jsonObject.getJSONArray("objects");

        if (CollectionUtils.isEmpty(objectsJSONArray)) {
            return;
        }

        ///
        /// 获取第一条新闻，判断新闻是否有更新

        JSONObject firstObject = objectsJSONArray.getJSONObject(0);
        JSONObject firstPost = firstObject.getJSONObject("post");
        String firstPostId = String.valueOf(firstPost.getInteger("post_id"));

        String oldPostId = null;
        if (CACHE_POSITION.containsKey(CACHE_POSITION_KEY)) {
            String cachePostId = CACHE_POSITION.get(CACHE_POSITION_KEY);
            if (CACHE_POSITION.get(CACHE_POSITION_KEY).equals(firstPostId)) {
                // 没有更新 skip
                return;
            }

            // 有更新
            oldPostId = cachePostId;
            CACHE_POSITION.put(CACHE_POSITION_KEY, firstPostId);
        } else {
            CACHE_POSITION.put(CACHE_POSITION_KEY, firstPostId);
        }


        List<NewsDO> newsDOList = Lists.newArrayList();
        Map<Long, NewsImageDO> newsImageDOMap = Maps.newHashMap();
        Map<Long, NewsContentArticleDO> articleDOMap = Maps.newHashMap();
        for (int i = 0; i < objectsJSONArray.size(); i++) {
            JSONObject obj = objectsJSONArray.getJSONObject(i).getJSONObject("post");

            // 基本信息
            String title = obj.getString("post_title");
            String postId = obj.getString("post_id");
            if (StringUtils.isEmpty(postId)) {
                continue;
            }

            // 检查是否，已经抓取到 old 的新闻了
            // 到了直接return，不需要再抓取下去了
            if (oldPostId != null && oldPostId.equals(postId)) {
                return;
            }

            // keywords
            JSONArray tagJSONArray = obj.getJSONArray("post_tag");
            String keywords = "";
            if (!CollectionUtil.isEmpty(tagJSONArray)) {
                for (Object tagObject : tagJSONArray) {
                    keywords = String.valueOf(tagObject) + ",";
                }
                keywords = keywords.substring(0, keywords.length() - 1);
            }

            Date createTime = new Date(obj.getLong("created_at") * 1000);
            int sourceCommentCount = obj.getInteger("comment_count");
            String article = obj.getString("post_content");

            if (StringUtils.isEmpty(article)) {
                return;
            }

            String newsAbstract = obj.getString("post_excerpt");
            String newsSourceUrl = obj.getString("post_url");
            String dataKey = encrypt(newsSourceUrl);

            // 处理封面图
            String imageUrl = obj.getString("post_cover_image");
            Integer imageWidth;
            Integer imageHeight;
            int imageCount = 1;
            Map<String, Integer> convertImageSizeMap = schedulerUtils.parseImg(imageUrl);
            if (CollectionUtils.isEmpty(convertImageSizeMap)) {
                imageHeight = 0;
                imageWidth = 0;
            } else {
                imageWidth = convertImageSizeMap.get("width");
                imageHeight = convertImageSizeMap.get("height");
            }

            ///
            /// 构建 newsDO

            long newsId = IDUtil.getNewID();
            int newsHot = 0;
            String newsUrl = "";
            String shareUrl = "";
            int newsType = NewsDO.NEWS_TYPE_TECHNOLOGY;
            int contentType = NewsDO.CONTENT_TYPE_IMAGE_TEXT;
            String newsSource = NewsDO.DATA_SOURCE_IFANR;
            int banComment = 0;
            Date displayTime = cn.mc.core.utils.DateUtil.currentDate();
            int videoCount = 0;

            // 默认一张图片
            int displayType = handleNewsDisplayType(imageCount);
            int commentCount = 0;

            NewsDO newsDO = newsCoreManager.buildNewsDO(newsId, dataKey, title, newsHot,
                    newsUrl, shareUrl, newsSource, newsSourceUrl, newsType,
                    contentType, keywords, banComment, newsAbstract, displayTime,
                    videoCount, imageCount, createTime, displayType,
                    sourceCommentCount, commentCount);

           newsDOList.add(newsDO);

            ///
            /// 构建一个 newsImageDO

            NewsImageDO newsImageDO = newsImageCoreManager.buildNewsImageDO(
                    IDUtil.getNewID(), newsId, imageUrl,
                    imageWidth, imageHeight, NewsImageDO.IMAGE_TYPE_MINI);

            newsImageDOMap.put(newsId, newsImageDO);

            ///
            /// 构建文章 newsContentArticle

            NewsContentArticleDO newsContentArticleDO = newsContentArticleCoreManager
                    .buildNewsContentArticleDO(IDUtil.getNewID(), newsId, article);

            articleDOMap.put(newsId, newsContentArticleDO);
        }

        // 去保存

        ifanrPipeline.save(newsDOList, newsImageDOMap, articleDOMap);
    }
}
