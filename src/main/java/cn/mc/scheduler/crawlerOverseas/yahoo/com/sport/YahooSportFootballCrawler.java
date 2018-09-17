package cn.mc.scheduler.crawlerOverseas.yahoo.com.sport;

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
import org.springframework.util.CollectionUtils;
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
 * YahooSport - 足球新闻
 *
 * Created by dai on 2018/9/12.
 */
@Component
public class YahooSportFootballCrawler extends BaseCrawler {

    private static final Logger LOG = LoggerFactory.getLogger(YahooSportHomeCrawler.class);
    private static final String URL = "https://sports.yahoo.com/soccer/";
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
        LOG.info("抓取 Yahoo 足球新闻 begin!");
        Selectable sports = page.getHtml().xpath("//div[@id='Col1-1-SportsStream']");
        List<Selectable> nodes = sports.xpath("//ul[@class='Mb(0)']").$(".js-stream-content").nodes();

        if (CollectionUtil.isEmpty(nodes)) {
            LOG.error("Yahoo 足球 node 为空");
        }

        for (Selectable node : nodes) {
            Selectable Py = node.xpath("//div[@class='Py(14px)']/div");
            // 排除广告
            if (StringUtils.isEmpty(Py.toString())) {
                continue;
            }

            Selectable Cf = Py.xpath("//div[@class='Cf']/");
            Selectable Fl = Cf.xpath("//div[@class='Fl(start)']");
            // 排除无封面图
            if (StringUtils.isEmpty(Fl)) {
                continue;
            }

            String imageUrl = Py.xpath("//div[@class='Fl(start)']/div/img/@src").toString();

            // 标题 div
            Selectable Ov = Py.xpath("//div[@class='Ov(h)']");
            String originUrl = Ov.xpath("//h3/a/@href").toString();
            originUrl = "https://sports.yahoo.com" + originUrl;
            String title = Ov.xpath("//h3/a/text()").toString();
            String newsAbstract = Ov.xpath("//p/text()").toString();
            String dataKey = encrypt(originUrl);

            Integer imageWidth;
            Integer imageHeight;
            Map<String, Integer> imageSizeMap = schedulerUtils.parseImg(imageUrl);
            if (CollectionUtils.isEmpty(imageSizeMap)) {
                imageHeight = 0;
                imageWidth = 0;
            } else {
                imageWidth = imageSizeMap.get("width");
                imageHeight = imageSizeMap.get("height");
            }

            Long newsId = IDUtil.getNewID();
            int imageCount = 1;
            int newsHot = 0;
            String newsUrl = originUrl;
            String shareUrl = originUrl;
            int newsType = NewsDO.NEWS_TYPE_SPORT_OVERSEAS;
            int contentType = NewsDO.CONTENT_TYPE_IMAGE_TEXT;
            int banComment = 0;
            Date displayTime = DateUtil.currentDate();
            int videoCount = 0;
            int displayType = NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE;
            int commentCount = 0;
            int sourceCommentCount = 0;
            String keywords = "";
            Date createTime = DateUtil.currentDate();
            String newsSource = NewsDO.DATA_SOURCE_YAHOO_SPORT_FOOTBALL;

            // 新闻列表
            NewsDO newsDO = newsCoreManager.buildNewsDO(newsId, dataKey, title,
                    newsHot, newsUrl, shareUrl, newsSource, originUrl,
                    newsType, contentType, keywords, banComment, newsAbstract,
                    displayTime, videoCount, imageCount, createTime,
                    displayType, sourceCommentCount, commentCount);

            // 列表图片
            NewsImageDO newsImageDO = newsImageCoreManager.buildNewsImageDO(
                    IDUtil.getNewID(), newsId, imageUrl, imageWidth, imageHeight, NewsImageDO.IMAGE_TYPE_LARGE);
            List<NewsImageDO> newsImageDOList = Lists.newArrayList(newsImageDO);

            // 添加至缓存
            cacheNewsMap.put(dataKey, newsDO);
            cacheNewsImageMap.put(dataKey, newsImageDOList);

            // 跳转详情 url
            page.addTargetRequest(originUrl);

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
        String article = page.getHtml().xpath("//div[@id='Col1-0-ContentCanvas']/article").toString();

        // 去掉 <a> 标签
        article = schedulerUtils.replaceLabel(article);

        NewsDO newsDO = cacheNewsMap.get(dataKey);
        List<NewsImageDO> newsImageDOList = cacheNewsImageMap.get(dataKey);

        // 构建海外内容 article
        long newsId = newsDO.getNewsId();
        String title = newsDO.getTitle();
        NewsContentOverseasArticleDO overseasArticleDO
                = overseasArticleCoreManager.buildOverseasArticleDO(IDUtil.getNewID(), newsId, article, title);

        // 去 save 数据
        overseasCrawlerPipeline.save(newsDO, newsImageDOList, overseasArticleDO);

    }


}
