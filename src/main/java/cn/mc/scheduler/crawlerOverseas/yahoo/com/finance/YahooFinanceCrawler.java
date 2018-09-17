package cn.mc.scheduler.crawlerOverseas.yahoo.com.finance;

import cn.mc.core.dataObject.NewsContentOverseasArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsContentOverseasArticleCoreManager;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.CollectionUtil;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.crawlerOverseas.OverseasCrawlerPipeline;
import cn.mc.scheduler.util.SchedulerUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Selectable;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * yahoo 财经新闻
 *
 * Created by dai on 2018/9/11.
 */
@Component
public class YahooFinanceCrawler extends BaseCrawler {

    private static final Logger LOG = LoggerFactory.getLogger(YahooFinanceCrawler.class);
    private static final String URL = "https://finance.yahoo.com/";
    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(500);

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsContentOverseasArticleCoreManager overseasArticleCoreManager;
    @Autowired
    private OverseasCrawlerPipeline overseasCrawlerPipeline;
    @Autowired
    private SchedulerUtils schedulerUtils;

    private Map<String, NewsDO> cacheNewsMap = Maps.newHashMap();
    private Map<String, List<NewsImageDO>> cacheNewsImageMap = Maps.newHashMap();

    @Override
    public Spider createCrawler() {
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(new Request(URL));
        return spider;
    }

    @Override
    public void process(Page page) {
        if (URL.equals(page.getUrl().toString())) {
            crawlerList(page);
        } else {
            crawlerDetails(page);
        }
    }

    @Override
    public Site getSite() {
        return SITE;
    }

    /**
     * 抓取新闻列表
     *
     * @param page
     */
    private void crawlerList(Page page) {
        LOG.info("抓取 Yahoo 财经新闻 begin!");
        Selectable sling = page.getHtml().xpath("//div[@id='slingstoneStream-0-Stream']");
        List<Selectable> newsNodes = sling.xpath("//ul/li").nodes();

        if (CollectionUtil.isEmpty(newsNodes)) {
            LOG.error("Yahoo 财经新闻 node 为空");
        }

        for (Selectable newsNode : newsNodes) {

            // 获取图片
            String title = newsNode.xpath("//h3[@class='Mb(5px)']/a/allText()").toString();
            String newsSourceUri = newsNode.xpath("//h3[@class='Mb(5px)']/a/@href").toString();
            String newsAbstract = newsNode.xpath("//div[@class='Cf']/div[2]/p/allText()").toString();
            String label = newsNode.xpath("//div[@class='Cf']/div[2]/div[1]/allText()").toString();
            String newsAuthor = newsNode.xpath("//div[@class='Cf']/div[2]/div[2]/allText()").toString();

            // 获取所有的图片
            List<Selectable> imageNodes = newsNode.xpath("//img/@src").nodes();

            if (StringUtils.isEmpty(title)
                    || StringUtils.isEmpty(newsSourceUri)
                    || StringUtils.isEmpty(newsAbstract)
                    || StringUtils.isEmpty(label)
                    || StringUtils.isEmpty(newsAuthor)) {
                continue;
            }

            // 排除广告
            if (label.equals("Sponsored")) {
                continue;
            }

            Long newsId = IDUtil.getNewID();
            String newsSourceUrl = URL + newsSourceUri;
            String dataKey = encrypt(newsSourceUrl);


            ///
            /// 处理封面图

            List<NewsImageDO> newsImageDOList = Lists.newArrayList();
            for (Selectable imageNode : imageNodes) {
                String imageUrl = imageNode.toString();
                int imageWidth = 0;
                int imageHeight = 0;
                int imageType = NewsImageDO.IMAGE_TYPE_LARGE;
                NewsImageDO imageDO = newsImageCoreManager
                        .buildNewsImageDO(IDUtil.getNewID(),
                        newsId, imageUrl, imageWidth, imageHeight, imageType);
                newsImageDOList.add(imageDO);
            }

            int newsHot = 0;
            String newsUrl = "";
            String shareUrl = "";
            int newsType = NewsDO.NEWS_TYPE_FINANCE_OVERSEAS;
            int contentType = NewsDO.CONTENT_TYPE_IMAGE_TEXT;
            int banComment = 0;
            Date displayTime = DateUtil.currentDate();
            int videoCount = 0;
            int imageCount = newsImageDOList.size();

            // 显示类型
            int displayType;
            if (newsImageDOList.size() >= 3) {
                displayType = NewsDO.DISPLAY_TYPE_THREE_MINI_IMAGE;
            } else {
                displayType = NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE;
            }

            int commentCount = 0;
            int sourceCommentCount = 0;
            String keywords = "";
            Date createTime = DateUtil.currentDate();
            String newsSource = NewsDO.DATA_SOURCE_YAHOO_FINANCE + " - " + newsAuthor;

            // 新闻列表
            NewsDO newsDO = newsCoreManager.buildNewsDO(newsId, dataKey, title,
                    newsHot, newsUrl, shareUrl, newsSource, newsSourceUrl,
                    newsType, contentType, keywords, banComment, newsAbstract,
                    displayTime, videoCount, imageCount, createTime,
                    displayType, sourceCommentCount, commentCount);

            // 添加至缓存
            cacheNewsMap.put(dataKey, newsDO);
            cacheNewsImageMap.put(dataKey, newsImageDOList);

            // 跳转详情url
            page.addTargetRequest(newsSourceUrl);
        }
    }

    /**
     * 抓取新闻详情
     *
     * @param page
     */
    private void crawlerDetails(Page page) {
        String url = page.getUrl().toString();
        String dataKey = encrypt(url);
        String content = page.getHtml().xpath("//div[@id='Col1-0-ContentCanvas']/article").toString();

        // 去掉 <a> 标签
        content = schedulerUtils.replaceLabel(content);
        // 过滤 标签中的 class id style 等.. 操作
        content = schedulerUtils.filterHtmlLabel(content);

        // 获取 newsDO
        NewsDO newsDO = cacheNewsMap.get(dataKey);
        List<NewsImageDO> newsImageDOList = cacheNewsImageMap.get(dataKey);

        // 构建海外内容 article
        long newsId = newsDO.getNewsId();
        String title = newsDO.getTitle();
        NewsContentOverseasArticleDO overseasArticleDO
                = overseasArticleCoreManager.buildOverseasArticleDO(
                        IDUtil.getNewID(), newsId, content, title);

        // 去 save 数据
        overseasCrawlerPipeline.save(newsDO, newsImageDOList, overseasArticleDO);
    }
}
