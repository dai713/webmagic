package cn.mc.scheduler.crawler.huxiu.com;

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

/**
 * 虎嗅网新闻
 *
 * @author daiqingwen
 * @date 2018/7/24 上午 11:35
 */

@Component
public class HuXiuCrawlers extends BaseCrawler {

    private static final Logger log = LoggerFactory.getLogger(HuXiuCrawlers.class);

    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(1000);

    private static final String url = "https://m.huxiu.com/";

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private SchedulerUtils schedulerUtils;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private HuXiuPipeline huXiuPipeline;

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
        log.info("开始抓取虎嗅新闻...");
        try {
            List<Selectable> nodes = page.getHtml().xpath("//ul[@class='js-article-append-box']/li").nodes();

            // 获取第一条数据
            Selectable xpath = nodes.get(0);
            Selectable label = xpath.xpath("//a");
            String firstUrl;
            String firstId;
            String http = "https://m.huxiu.com";
            // 判断是小图还是大图
            if (StringUtils.isEmpty(label)) {
                // 大图
                List<Selectable> a_label = xpath.xpath("/div[@class='article-hp-big-info']/a").nodes();
                firstUrl = http + a_label.get(1).xpath("//a/@href").toString();
                firstId = xpath.xpath("//div[@class='article-hp-big-info']/@data-id").toString();
            } else {
                // 小图
                firstUrl = http + xpath.xpath("//div[@class='rec-article-info']/a/@href").toString();
                firstId = firstUrl.substring(firstUrl.lastIndexOf("/") + 1, firstUrl.length() - 5);
            }
            String cache = (String) redisUtil.get(firstId);
            if (!StringUtils.isEmpty(cache)) {
                if (cache.equals(firstUrl)) {
                    return;
                } else {
                    redisUtil.setString(firstId, firstUrl);
                }
            } else {
                redisUtil.setString(firstId, firstUrl);
            }

            List<String> urlList = Lists.newArrayList();

            for (int i = 0; i < nodes.size(); i++) {
                Selectable selectable = nodes.get(i);
                Selectable aLabel = selectable.xpath("//a");

                String title;
                String url;
                String imgUrl;
//                Integer collection = 0; // 点赞
                Integer displayType;
                Integer imgType;

                // 判断大图还是小图
                if (StringUtils.isEmpty(aLabel)) {
                    // 大图
                    List<Selectable> bigImg = selectable.xpath("/div[@class='article-hp-big-info']/a").nodes();
                    url = http + bigImg.get(0).xpath("//@href").toString();
                    imgUrl = bigImg.get(0).xpath("//img/@src").toString();
                    title = bigImg.get(1).xpath("//h2/text()").toString();
//                    Selectable collect =  selectable.xpath("//div[@class='article-hp-big-info']/div");
//                    String num = collect.xpath("//span[@class='fr']/text()").toString();
//                    collection = Integer.parseInt(collect.substring(0, collect.length() - 3));
                    displayType = NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE;
                    imgType = NewsImageDO.IMAGE_TYPE_LARGE;
                } else {
                    title = selectable.xpath("//div[@class='rec-article-info']/a/h2/span/text()").toString();
                    url = http + selectable.xpath("//a/@href").toString();
                    imgUrl = selectable.xpath("//a/img/@data-original").toString();
//                    Selectable collect = selectable.xpath("//div[@class='rec-article-info']/div");
//                    String num = collect.xpath("//span[@class='fr']/text()").toString();
//                    collection = Integer.parseInt(collect.substring(0, collect.length() - 3));
                    displayType = NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE;
                    imgType = NewsImageDO.IMAGE_TYPE_MINI;
                }
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

                NewsDO newsDO = newsCoreManager.buildNewsDO(newId, dataKey, title,
                        0, url, "", NewsDO.DATA_SOURCE_HUXIU, url,
                        NewsDO.NEWS_TYPE_TECHNOLOGY, NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                        "", 0, "", date,
                        0, 1, date, displayType,
                        0, 0);
                list.add(newsDO);

                // 上传图片至阿里云
                String saveImg = aliyunOSSClientUtil.uploadPicture(imgUrl);
//                String saveImg = imgUrl;

                NewsImageDO newsImage = new NewsImageDO(IDUtil.getNewID(), newId,
                        srcWidth, srcHeight, imgType, saveImg, NewsImageDO.STATUS_NORMAL);
                imageList.add(newsImage);

            }

            page.addTargetRequests(urlList);

        } catch (Exception e) {
            log.error("抓取虎嗅新闻失败：{}", e);
        }

    }

    /**
     * 获取新闻详情
     * @param page
     */
    private void getDetails(Page page) {
        String content = page.getHtml().xpath("//div[@class='article-box']").toString();

        content = schedulerUtils.replaceLabel(content);
        String originUrl = page.getUrl().toString();
        Long newsId = 0L;
        for (NewsDO newsDO : list) {
            String newsUrl = newsDO.getNewsUrl();
            if (originUrl.equals(newsUrl)) {
                newsId = newsDO.getNewsId();
                break;
            }
        }

        NewsContentArticleDO contentArticleDO = new NewsContentArticleDO(IDUtil.getNewID(), newsId, content, NewsContentArticleDO.ARTICLE_TYPE_HTML);
        detailList.add(contentArticleDO);

        if (list.size() == detailList.size()) {
            huXiuPipeline.save(list, detailList, imageList);
        }

    }
}
