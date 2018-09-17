package cn.mc.scheduler.crawler.engadget.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsContentArticleCoreManager;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.util.SchedulerUtils;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * engadget 新闻
 *
 * @author daiqingwen
 * @date 2018/7/24 下午 19:45
 */
@Component
public class EngadgetCrawlers extends BaseCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngadgetCrawlers.class);

    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(500);

    private static final String url = "https://cn.engadget.com/topics/science/";
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

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsContentArticleCoreManager newsContentArticleCoreManager;
    @Autowired
    private SchedulerUtils schedulerUtils;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private EngadgetPipeline engadgetPipeline;

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
     *
     * @param page
     */
    private void getList(Page page) {
        LOGGER.info("开始抓取engadget新闻...");
        Selectable xpath = page.getHtml().xpath("//div[@id='latest']/div");
        xpath = xpath.xpath("//div[@class='flex-1']");
        List<Selectable> nodes = xpath.xpath("//div[@class='container@m']").nodes();
        String host = "https://cn.engadget.com";

        // 获取第一条数据
        Selectable firstSelectable = nodes.get(0).xpath("//article/div");
        firstSelectable = firstSelectable.xpath("//div[@class='col-4-of-4@tp']/div");
        // 标题描述div
        Selectable descriptionSelect = firstSelectable.xpath("//div[@class='o-thumb_overlay_desc@tp+']/div/div/div");
        String firstTitle = descriptionSelect.xpath("//div[@class='th-title']/h2/a/span/text()").toString();

        String oldTitle = null;
        if (CACHE_POSITION.containsKey(CACHE_POSITION_KEY)) {
            String title = CACHE_POSITION.get(CACHE_POSITION_KEY);
            if (title.equals(firstTitle)) {
                // 没有更新 skip
                return;
            }

            // 有更新
            oldTitle = title;
            CACHE_POSITION.put(CACHE_POSITION_KEY, firstTitle);
        } else {
            CACHE_POSITION.put(CACHE_POSITION_KEY, firstTitle);
        }

        for (int i = 1; i < nodes.size(); i++) {
            Selectable selectable = nodes.get(i).xpath("//article/div");
            Selectable hrefImgSelect = selectable.xpath("//div[@class='col-4-of-11@d']");
            // a标签div
            Selectable hrefSelectable = hrefImgSelect.xpath("//div[@class='o-rating_thumb']");
            // 标题描述div
            Selectable descriptionSelectable = selectable.xpath("//div[@class='col-6-of-11@d']/div/div/div");

            String newsSourceUrl = host + hrefSelectable.xpath("//a/@href").toString();
            String imageUrl = hrefSelectable.xpath("//a/img/@data-original").toString();
            String title = descriptionSelectable.xpath("//div[@class='th-title']/h2/span/text()").toString();
            String newsAbstract = descriptionSelectable.xpath("//div[@class='mt-10@s']/p/text()").toString();

            if (oldTitle != null && oldTitle.equals(title)) {
                // oldTitle 如果等于 title，旧数据
                return;
            }

            Long newsId = IDUtil.getNewID();
            String dataKey = encrypt(newsSourceUrl);

            Integer imageWidth;
            Integer imageHeight;
            Map<String, Integer> imageSizeMap = schedulerUtils.parseImg(imageUrl);
            int imageCount = 1;
            if (CollectionUtils.isEmpty(imageSizeMap)) {
                imageHeight = 0;
                imageWidth = 0;
            } else {
                imageWidth = imageSizeMap.get("width");
                imageHeight = imageSizeMap.get("height");
            }

            ///
            /// 构建 newsDO

            int newsHot = 0;
            String newsUrl = "";
            String shareUrl = "";
            int newsType = NewsDO.NEWS_TYPE_TECHNOLOGY;
            int contentType = NewsDO.CONTENT_TYPE_IMAGE_TEXT;
            int banComment = 0;
            Date displayTime = cn.mc.core.utils.DateUtil.currentDate();
            int videoCount = 0;

            // displayType 图片够大，直接大图显示
            int displayType = NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE;

            int commentCount = 0;
            int sourceCommentCount = 0;
            String keywords = "";
            Date createTime = DateUtil.currentDate();
            String newsSource = NewsDO.DATA_SOURCE_ENGADGET;

            NewsDO newsDO = newsCoreManager.buildNewsDO(newsId, dataKey, title, newsHot,
                    newsUrl, shareUrl, newsSource, newsSourceUrl, newsType,
                    contentType, keywords, banComment, newsAbstract, displayTime,
                    videoCount, imageCount, createTime, displayType,
                    sourceCommentCount, commentCount);

            newsMap.put(dataKey, newsDO);

            ///
            /// 构建 newsImage

            NewsImageDO newsImageDO = newsImageCoreManager.buildNewsImageDO(
                    IDUtil.getNewID(), newsId,
                    imageUrl, imageWidth, imageHeight, NewsImageDO.IMAGE_TYPE_LARGE);

            imageMap.put(dataKey, newsImageDO);

            // 添加 targetRequest
            page.addTargetRequest(new Request(newsSourceUrl));
        }
    }

    /**
     * 获取新闻详情
     *
     * @param page
     */
    private void getDetails(Page page) {

        String pageUrl = page.getUrl().toString();
        String dataKey = this.encrypt(pageUrl);
        Html htmlNode = page.getHtml();

        // 获取文章信息，此处文章分为两段
        List<Selectable> articleNodes = htmlNode.xpath("//div[@class='article-text']").nodes();
        String articleHtml = "";
        for (Selectable articleNode : articleNodes) {
            articleHtml += articleNode.xpath("//html()").toString();
        }

        // 新闻的图片，有点特殊，和文章是分开的
        // 单独获取图片，拼接到 articleHtml
        List<String> stretchImageNodes = page.getHtml()
                .xpath("//div[@class='hide@tp+']/img[@class='stretch-img']").all();

        if (!CollectionUtils.isEmpty(stretchImageNodes)) {
            String imageHtml = "";
            for (String stretchImageNode : stretchImageNodes) {
                imageHtml += stretchImageNode;
            }
            articleHtml = imageHtml + articleHtml;
        }

        // 替换</a>标签
        articleHtml = schedulerUtils.replaceLabel(articleHtml);

        if (!newsMap.containsKey(dataKey)) {
            return;
        }

        NewsDO newsDO = newsMap.get(dataKey);
        Long newsId = newsDO.getNewsId();

        // 构建 article
        NewsContentArticleDO newsContentArticleDO = newsContentArticleCoreManager
                .buildNewsContentArticleDO(IDUtil.getNewID(), newsId, articleHtml);

        // 保存新闻
        engadgetPipeline.toSave(dataKey, newsMap, imageMap, newsContentArticleDO);
    }
}
