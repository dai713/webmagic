package cn.mc.scheduler.crawler.engadget.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import cn.mc.scheduler.util.RedisUtil;
import cn.mc.scheduler.util.SchedulerUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
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

@Component
public class EngadgetCrawlers extends BaseCrawler {

    private static final Logger log = LoggerFactory.getLogger(EngadgetCrawlers.class);

    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(1000);

    private static final String url = "https://cn.engadget.com/topics/science/";

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private SchedulerUtils schedulerUtils;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private EngadgetPipeline engadgetPipeline;

    private static List<NewsDO> list = Lists.newArrayList();
    private static List<NewsContentArticleDO> detailList = Lists.newArrayList();
    private static List<NewsImageDO> imageList = Lists.newArrayList();

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
     * @param page
     */
    private void getList(Page page) {
        log.info("开始抓取engadget新闻...");
        try {
            Selectable xpath = page.getHtml().xpath("//div[@id='latest']/div");
            xpath = xpath.xpath("//div[@class='flex-1']");
            List<Selectable> nodes =xpath.xpath("//div[@class='container@m']").nodes();
            String http = "https://cn.engadget.com";

            // 获取第一条数据
            Selectable firstSelectable = nodes.get(0).xpath("//article/div");
            firstSelectable = firstSelectable.xpath("//div[@class='col-4-of-4@tp']/div");
            // a标签div
            Selectable hrefSelect = firstSelectable.xpath("//div[@class='o-rating_thumb@m-']");
            // 标题描述div scheduler/src/test/java/cn/mc/scheduler/crawler/CrawlerTest.java
            Selectable descriptionSelect = firstSelectable.xpath("//div[@class='o-thumb_overlay_desc@tp+']/div/div/div");
            String firstUrl = http + hrefSelect.xpath("//a/@href").toString();
            String firstTitel = descriptionSelect.xpath("//div[@class='th-title']/h2/a/span/text()").toString();

            String cache = (String) redisUtil.get(firstTitel);
            if (!StringUtils.isEmpty(cache)) {
                if (cache.equals(firstUrl)) {
                    return;
                } else {
                    redisUtil.setString(firstTitel, firstUrl);
                }
            } else {
                redisUtil.setString(firstTitel, firstUrl);
            }

            List<String> urlList = Lists.newArrayList();

            for (int i = 1; i < nodes.size(); i++) {
                Selectable selectable = nodes.get(i).xpath("//article/div");
                Selectable href_img_select = selectable.xpath("//div[@class='col-4-of-11@d']");
                // a标签div
                Selectable hrefSelectable = href_img_select.xpath("//div[@class='o-rating_thumb']");
                // 标题描述div
                Selectable descriptionSelectable = selectable.xpath("//div[@class='col-6-of-11@d']/div/div/div");

                String url = http + hrefSelectable.xpath("//a/@href").toString();
                String imgUrl = hrefSelectable.xpath("//a/img/@data-original").toString();
                String title = descriptionSelectable.xpath("//div[@class='th-title']/h2/span/text()").toString();
                String newsAbstract = descriptionSelectable.xpath("//div[@class='mt-10@s']/p/text()").toString();

                Long newId = IDUtil.getNewID();
                String dataKey = this.encrypt(String.valueOf(IDUtil.getNewID()));
                Date date = new Date();

                Integer srcWidth;
                Integer srcHeight;
                Map map = schedulerUtils.parseImg(imgUrl);
                if (StringUtils.isEmpty(map)) {
                    srcHeight = 0;
                    srcWidth = 0;
                } else {
                    srcWidth = (Integer) map.get("width");
                    srcHeight = (Integer) map.get("height");
                }

                urlList.add(url);

                NewsDO newsDO = newsCoreManager.buildNewsDO(newId, dataKey, title, 0, url, "", NewsDO.DATA_SOURCE_ENGADGET, url, NewsDO.NEWS_TYPE_TECHNOLOGY,
                        NewsDO.CONTENT_TYPE_IMAGE_TEXT,"", 0, newsAbstract, date, 0, 1, date, NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE,
                        0, 0);
                list.add(newsDO);

                // 上传图片至阿里云
                String saveImg = aliyunOSSClientUtil.uploadPicture(imgUrl);
//                String saveImg = imgUrl;

                NewsImageDO newsImage = new NewsImageDO(IDUtil.getNewID(), newId, srcWidth, srcHeight, NewsImageDO.IMAGE_TYPE_LARGE, saveImg, NewsImageDO.STATUS_NORMAL);
                imageList.add(newsImage);

            }

            page.addTargetRequests(urlList);

        } catch (Exception e) {
            log.error("抓取engadget新闻失败：{}" + e);
        }

    }

    /**
     * 获取新闻详情
     * @param page
     */
    private void getDetails(Page page) {
        Selectable selectable = page.getHtml().xpath("//div[@id='page_body']/div/div");
        String content = selectable.xpath("//div[@class='pb-15']").toString();
        String originUrl = page.getUrl().toString();

        content = schedulerUtils.replaceLabel(content);
        Long newsId = 0L;
        for (NewsDO news : list) {
            String newsUrl = news.getNewsUrl();
            if (originUrl.equals(newsUrl)) {
                newsId = news.getNewsId();
                break;
            }
        }
        NewsContentArticleDO contentArticleDO = new NewsContentArticleDO(IDUtil.getNewID(), newsId, content, NewsContentArticleDO.ARTICLE_TYPE_HTML);
        detailList.add(contentArticleDO);

        if (list.size() == detailList.size()) {
            engadgetPipeline.save(list, detailList, imageList);
        }

    }
}
