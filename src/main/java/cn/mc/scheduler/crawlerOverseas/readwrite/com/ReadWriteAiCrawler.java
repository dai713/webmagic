package cn.mc.scheduler.crawlerOverseas.readwrite.com;

import cn.mc.core.dataObject.NewsContentOverseasArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsContentOverseasArticleCoreManager;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.HtmlNodeUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.core.utils.MessageUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.crawlerOverseas.OverseasCrawlerPipeline;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import us.codecraft.webmagic.*;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * readwrite.com - ai 爬虫
 *
 * @author Sin
 * @time 2018/8/28 下午8:11
 */
@Component
public class ReadWriteAiCrawler extends BaseCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadWriteAiCrawler.class);
    private static final String URL = "http://readwrite.com/category/ai/";
    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(300);

    private Map<String, NewsDO> cacheNewsMap = Maps.newHashMap();
    private Map<String, List<NewsImageDO>> cacheNewsImageMap = Maps.newHashMap();

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsContentOverseasArticleCoreManager overseasArticleCoreManager;
    @Autowired
    private OverseasCrawlerPipeline overseasCrawlerPipeline;

    @Override
    public Spider createCrawler() {
        Request request = new Request(URL);
        addHeader(request);
        return new BaseSpider(this).addRequest(request);
    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();
        if (url.equals(URL)) {
            handleList(page);
        } else {
            handleDetail(page);
        }
    }

    @Override
    public Site getSite() {
        return SITE;
    }

    ///
    /// 内容处理 z

    void handleDetail(Page page) {
        String url = page.getUrl().toString();
        String dataKey = encrypt(url);
        Html html = page.getHtml();
        ResultItems resultItems  = page.getResultItems();

        // 获取文章内容 node
        Selectable contentNode = html.xpath("//div[@class='entry-content']");

        // 需要清楚的 nodes
        List<String> clearXpathList = ImmutableList.of(
                "div[@class='clearfix']",
                "div[@class='menu-social-media-container']",
                "div[@class='popular-tags']",
                "div[@class='about-author']",
                "div[@class='related_posts']"
        );

        for (String xpath : clearXpathList) {
            List<Selectable> nodes = contentNode.xpath(xpath).nodes();
            HtmlNodeUtil.removeLabel(nodes);
        }

        // 获取 clear 后的 node
        String articleHtml = contentNode.xpath("//div[@class='entry-content']/html()").toString();

        // 将封面图的图片插入到文章
        Map<String, Object> cacheObject = resultItems.get(dataKey);

        NewsDO newsDO = cacheNewsMap.get(dataKey);
        List<NewsImageDO> newsImageDOList = cacheNewsImageMap.get(dataKey);

        if (!CollectionUtils.isEmpty(newsImageDOList)) {
            NewsImageDO newsImageDO = newsImageDOList.get(0);
            String imageTemplate = MessageUtil.doFormat(
                    "<img src=\"{}\" />", newsImageDO.getImageUrl());

            articleHtml = imageTemplate + articleHtml;
        }

        // 构建海外内容 article
        long newsId = newsDO.getNewsId();
        String title = newsDO.getTitle();
        long newsContentOverseasArticleId = IDUtil.getNewID();
        NewsContentOverseasArticleDO overseasArticleDO
                = overseasArticleCoreManager.buildOverseasArticleDO(
                        newsContentOverseasArticleId,
                        newsId,
                        articleHtml,
                        title
                );

        // 去 save 数据
        overseasCrawlerPipeline.save(newsDO, newsImageDOList, overseasArticleDO);
    }

    void handleList(Page page) {
        Html html = page.getHtml();
        List<Selectable> sectionNodes = html.xpath("//*[@id='cat-latest']/div/div/section").nodes();

        if (CollectionUtils.isEmpty(sectionNodes)) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("section 没找到！爬虫异常！");
            }
        }

        for (Selectable sectionNode : sectionNodes) {
            Selectable imageNode = sectionNode.xpath("//div/div[1]/img");
            String imageUrl = imageNode.xpath("img/@src").toString();
            String title = sectionNode.xpath("//div/div[2]/h2/allText()").toString();
            String newsAbstract = sectionNode.xpath("//div/div[2]/div[2]/allText()").toString();
            String newsSource = sectionNode.xpath("//div/div[2]/div[1]/allText()").toString();
            String newsSourceUrl = sectionNode.xpath("//div/div[2]/h2/a/@href").toString();

            // 处理 newsSource
            if (newsSource.length() > 50) {
                newsSource = newsSource.substring(0, 50);
            }

            // 创建 newsId
            long newsId = IDUtil.getNewID();

            // 构建 newsImageDO
            long newsImageId = IDUtil.getNewID();
            int imageWidth = 0;
            int imageHeight = 0;
            NewsImageDO newsImageDO = newsImageCoreManager.buildNewsImageDO(
                    newsImageId, newsId,
                    imageUrl, imageWidth, imageHeight, NewsImageDO.IMAGE_TYPE_MINI);
            List<NewsImageDO> newsImageDOList = Lists.newArrayList(newsImageDO);

            // 构建 newsDO
            String dataKey = encrypt(newsSourceUrl);
            int newsHot = 0;
            String newsUrl = "";
            String shareUrl = "";
            int newsType = NewsDO.NEWS_TYPE_TECHNOLOGY_OVERSEAS;
            int contentType = NewsDO.CONTENT_TYPE_IMAGE_TEXT;
            int displayType = NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE;
            String keywords = "";
            int banComment = 0;
            Date displayTime = DateUtil.currentDate();
            int videoCount = 0;
            int imageCount = newsImageDOList.size();
            Date createTime = DateUtil.currentDate();
            int sourceCommentCount = 0;
            int commentCount = 0;

            NewsDO newsDO = newsCoreManager.buildNewsDO(newsId, dataKey, title,
                    newsHot, newsUrl, shareUrl, newsSource, newsSourceUrl,
                    newsType, contentType, keywords, banComment, newsAbstract,
                    displayTime, videoCount, imageCount, createTime,
                    displayType, sourceCommentCount, commentCount);

            // 添加到 cache
            cacheNewsMap.put(dataKey, newsDO);
            cacheNewsImageMap.put(dataKey, newsImageDOList);

            // 添加 detail 页面地址
            page.addTargetRequest(newsSourceUrl);
        }
    }


    ///
    /// tools

    void addHeader(Request request) {
        request.addHeader(":authority", "readwrite.com");
        request.addHeader(":method", "GET");
//        request.addHeader("content-type", "text/html; charset=UTF-8");
//        request.addHeader(":path", "/category/ai/");
        request.addHeader(":scheme", "http");
        request.addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
//        request.addHeader("accept-encoding", "gzip, deflate, br");
//        request.addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
//        request.addCookie("cookie", "__smVID=3b92e02817829a3dc3c2404f2dd0e50643d73f93e45e576b27ef3379408e3170; __cfduid=d3985d41236715d5fc47d2d56d66044091535458018; _ga=GA1.2.124868906.1535458020; _gid=GA1.2.1391071974.1535458020; __smVID=3b92e02817829a3dc3c2404f2dd0e50643d73f93e45e576b27ef3379408e3170; __smListBuilderShown=Tue%20Aug%2028%202018%2020:08:10%20GMT+0800%20(%E4%B8%AD%E5%9B%BD%E6%A0%87%E5%87%86%E6%97%B6%E9%97%B4); AWSELB=B5BBE1D30631B96E8F735FBE0F30FAF2CE76E2A6BFA6399B2EAF057E428ED47A097DE5F7EA9115A319A4E61702F78B25E9F2780C2793BEAD0CF3A2419AA5E984CFF8BB00A5; advanced_ads_browser_width=1491; __smToken=XXm8uQ6gORHIwZW9kgh9Ol6u; _gat=1");
//        request.addHeader("cache-control", "no-cache");
//        request.addHeader("referer", "https://readwrite.com/2018/08/23/need-to-grow-your-city-partnerships-are-the-key-to-success/");
        request.addHeader("upgrade-insecure-requests", "1");
        request.addHeader("user-agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1");
    }
}
