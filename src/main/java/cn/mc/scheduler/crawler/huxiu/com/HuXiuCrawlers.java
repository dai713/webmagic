package cn.mc.scheduler.crawler.huxiu.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsContentArticleCoreManager;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.CollectionUtil;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.util.HtmlNodeUtil;
import cn.mc.scheduler.util.SchedulerUtils;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;
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
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Selectable;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 虎嗅网新闻
 *
 * @author daiqingwen
 * @date 2018/7/24 上午 11:35
 */
@Component
public class HuXiuCrawlers extends BaseCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HuXiuCrawlers.class);

    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(300);

    private static final String url = "https://m.huxiu.com/";

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsContentArticleCoreManager newsContentArticleCoreManager;
    @Autowired
    private SchedulerUtils schedulerUtils;
    @Autowired
    private HuXiuPipeline huXiuPipeline;

    private Map<String, NewsDO> newsMap = Maps.newHashMap();
    private Map<String, NewsImageDO> imageMap = Maps.newHashMap();

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
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public void process(Page page) {
       try {
           if (url.equals(page.getUrl().toString())) {
               if (LOGGER.isDebugEnabled()) {
                   LOGGER.info("开始抓取虎嗅新闻...");
               }

               getList(page);
           } else {
               getDetails(page);
           }
       } catch (Exception e) {
           e.printStackTrace();
       }
    }

    @Override
    public Site getSite() {
        return SITE;
    }

    private static final String ARTICLE_TYPE_MIN_PICTURE_ARTICLE = "MIN_PICTURE_ARTICLE";
    private static final String ARTICLE_TYPE_MIN_PICTURE_VIDEO_ARTICLE = "MIN_PICTURE_VIDEO_ARTICLE";
    private static final String ARTICLE_TYPE_LARGE_PICTURE_ARTICLE= "LARGE_PICTURE_ARTICLE";

    /**
     * 获取新闻列表
     * @param page
     */
    private void getList(Page page) {

        Selectable htmlNode = page.getHtml();

        // 处理图片懒加载问题
        List<Selectable> imageNodes = htmlNode.xpath("//img").nodes();
        if (!CollectionUtils.isEmpty(imageNodes)) {
            HtmlNodeUtil.handleCdnWithRemoveSourceAttr(imageNodes, "data-original", "src");
        }

        List<Selectable> nodes = htmlNode.xpath("//ul[@class='js-article-append-box']/li").nodes();
        String oldDataId = null;
        for (int i = 0; i < nodes.size(); i++) {
            Selectable itemNode = nodes.get(i);

            String title;
            String newsSourceUrl;
            String imageUrl;
            Integer displayType;
            Integer imageType;
            String firstUrl;
            String dataId;

            // 判断大图还是小图
            String host = "https://m.huxiu.com/";
            String articleType = getArticleType(itemNode);

            if (articleType == null) {
                return;
            }

            if (ARTICLE_TYPE_LARGE_PICTURE_ARTICLE.equals(articleType)) {
                // 大图片文章
                List<Selectable> bigImageLinkNode = itemNode.xpath("div[@class='article-hp-big-info']/a").nodes();
                newsSourceUrl = host + bigImageLinkNode.get(0).xpath("//@href").toString();
                imageUrl = bigImageLinkNode.get(0).xpath("//img/@src").toString();
                title = bigImageLinkNode.get(1).xpath("//h2/text()").toString().trim();
                displayType = NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE;
                imageType = NewsImageDO.IMAGE_TYPE_LARGE;
                dataId = itemNode.xpath("//div[@class='article-hp-big-info']/@data-id").toString();
            } else {
                // 图片里面有文章、小图片里面视频
                title = itemNode.xpath("//div[@class='rec-article-info']/a/h2/span/text()").toString().trim();
                newsSourceUrl = host + itemNode.xpath("//a/@href").toString();
                imageUrl = itemNode.xpath("//a/img/@src").toString();
                displayType = NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE;
                imageType = NewsImageDO.IMAGE_TYPE_MINI;
                firstUrl = host + itemNode.xpath("//div[@class='rec-article-info']/a/@href").toString();
                dataId = firstUrl.substring(firstUrl.lastIndexOf("/") + 1, firstUrl.length() - 5);
            }

            if (i <= 0) {
                if (CACHE_POSITION.containsKey(CACHE_POSITION_KEY)) {
                    String cacheDataId = CACHE_POSITION.get(CACHE_POSITION_KEY);
                    if (cacheDataId.equals(dataId)) {
                        // 没有更新 skip
                        return;
                    }

                    // 有更新
                    oldDataId = cacheDataId;
                    CACHE_POSITION.put(CACHE_POSITION_KEY, dataId);
                } else {
                    CACHE_POSITION.put(CACHE_POSITION_KEY, dataId);
                }
            }

            if (oldDataId != null && dataId.equals(oldDataId)) {
                // dataId 如果等于 oldDataId，旧数据
                return;
            }

            Long newsId = IDUtil.getNewID();
            String dataKey = encrypt(newsSourceUrl);
            Integer imageWidth;
            Integer imageHeight;
            int imageCount = 1;
            Map<String, Integer> imageSizeMap = schedulerUtils.parseImg(imageUrl);
            if (CollectionUtil.isEmpty(imageSizeMap)) {
                imageHeight = 0;
                imageWidth = 0;
            } else {
                imageWidth = imageSizeMap.get("width");
                imageHeight = imageSizeMap.get("height");
            }

            String newsSource = itemNode.xpath("//span[@class='rec-author']/text()").toString().trim();
            if (StringUtils.isEmpty(newsSource)) {
                newsSource = NewsDO.DATA_SOURCE_HUXIU;
            }

            // 处理 news
            int newsHot = 0;
            String newsUrl = "";
            String shareUrl = "";
            int newsType = NewsDO.NEWS_TYPE_TECHNOLOGY;
            int contentType = NewsDO.CONTENT_TYPE_IMAGE_TEXT;
            int banComment = 0;
            Date displayTime = cn.mc.core.utils.DateUtil.currentDate();
            int videoCount = 0;
            int commentCount = 0;
            int sourceCommentCount = 0;

            String keywords = "";
            String newsAbstract = "";

            Date createTime = null;
            NewsDO newsDO = newsCoreManager.buildNewsDO(newsId, dataKey, title, newsHot,
                    newsUrl, shareUrl, newsSource, newsSourceUrl, newsType,
                    contentType, keywords, banComment, newsAbstract, displayTime,
                    videoCount, imageCount, createTime, displayType,
                    sourceCommentCount, commentCount);

            newsMap.put(dataKey, newsDO);

            // 构建 newsImageDO
            NewsImageDO newsImage = newsImageCoreManager.buildNewsImageDO(
                    IDUtil.getNewID(), newsId, imageUrl, imageWidth, imageHeight, imageType);

            imageMap.put(dataKey, newsImage);

            // 添加 request
            page.addTargetRequest(new Request(newsSourceUrl));
        }

    }

    /**
     * 获取新闻详情
     *
     * @param page
     */
    private void getDetails(Page page) {
        Selectable htmlNode = page.getHtml();

        Selectable titleNode = htmlNode.xpath("//div[@class='article-content-title-box']");
        String createTimeString = titleNode.xpath("//span[@class='fr']/text()").toString();

        Date createTime;
        if (StringUtils.isEmpty(createTimeString)) {
            createTime = DateUtil.currentDate();
        } else {
            createTime = DateUtil.parse(createTimeString, "yyyy-MM-dd HH:mm:ms");
        }

        // 处理 "文章里面有视频" 存在 "下载虎嗅app...." 文案
        List<Selectable> warpNodes = htmlNode.xpath("//dl-app-wrap").nodes();
        if (!CollectionUtils.isEmpty(warpNodes)) {
            for (Selectable warpNode : warpNodes) {
                List<Element> elements = HtmlNodeUtil.getElements((HtmlNode) warpNode);
                elements.get(0).remove();
            }
        }

        String content = htmlNode.xpath("//div[@class='article-box']").toString();
        content = schedulerUtils.replaceLabel(content);
        String originUrl = page.getUrl().toString();
        String dataKey = encrypt(originUrl);

        if (!newsMap.containsKey(dataKey)) {
            return;
        }

        NewsDO newsDO = newsMap.get(dataKey);

        // 补充信息
        newsDO.setCreateTime(createTime);

        // 构建 article
        NewsContentArticleDO articleDO = newsContentArticleCoreManager
                .buildNewsContentArticleDO(IDUtil.getNewID(), newsDO.getNewsId(), content);

        // 去保存
        huXiuPipeline.save(dataKey, newsMap, imageMap, articleDO);
    }


    ///
    /// tools

    /**
     * 获取文章 类型
     *
     * @param itemNode
     * @return
     */
    private @Nullable String getArticleType(@NotNull Selectable itemNode) {
        String articleType = null;
        List<Selectable> articleOrVideoNodes = itemNode.xpath("//a[@class='rec-article-pic']").nodes();
        if (!CollectionUtils.isEmpty(articleOrVideoNodes)) {
            // 文章、视频
            List<Selectable> videoNodes = articleOrVideoNodes.get(0).xpath("//i[@class='video-icon']").nodes();
            if (!CollectionUtils.isEmpty(videoNodes)) {
                // 视频
                articleType = ARTICLE_TYPE_MIN_PICTURE_VIDEO_ARTICLE;
            } else {
                articleType = ARTICLE_TYPE_MIN_PICTURE_ARTICLE;
            }
        }

        List<Selectable> largeArticleNodes = itemNode.xpath("//a[@class='article-hp-big-pic']").nodes();
        if (!CollectionUtils.isEmpty(largeArticleNodes)) {
            articleType = ARTICLE_TYPE_LARGE_PICTURE_ARTICLE;
        }

        if (articleType == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("文章类型不支持! html: {}", itemNode.xpath("//html()").toString());
            }
        }

        return articleType;
    }
}
