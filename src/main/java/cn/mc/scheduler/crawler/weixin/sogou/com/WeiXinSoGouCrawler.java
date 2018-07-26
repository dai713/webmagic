package cn.mc.scheduler.crawler.weixin.sogou.com;

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
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 微信-搜狗新闻
 *
 * @author daiqingwen
 * @date 2018/7/23 下午 17:46
 */

@Component
public class WeiXinSoGouCrawler extends BaseCrawler{

    private static final Logger LOGGER = LoggerFactory.getLogger(WeiXinSoGouCrawler.class);

    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(300);

    private static final String url = "http://weixin.sogou.com/";

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private SchedulerUtils schedulerUtils;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private WeiXinSoGouPipeline weiXinSoGouPipeline;

    private List<NewsDO> list = Lists.newArrayList();
    private List<NewsContentArticleDO> detailList = Lists.newArrayList();
    private List<NewsImageDO> imageList = Lists.newArrayList();

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

    /**
     * 获取新闻列表
     * @param page
     */
    private void getList(Page page) {
        try {
            Html html = page.getHtml();
            List<Selectable> nodes = html.xpath("//ul[@id='pc_0_0']/li").nodes();
            List<String> urlList = Lists.newArrayList();
            // 获取第一条数据
            Selectable select = nodes.get(0).xpath("//div[@class='txt-box']");
            String firstTitel = select.xpath("//h3/a/text()").toString();
            String firstHref = select.xpath("//a/@href").toString();

            String cache = (String) redisUtil.get(firstTitel);
            if (!StringUtils.isEmpty(cache)) {
                if (cache.equals(firstHref)) {
                    return;
                } else {
                    redisUtil.setString(firstTitel, firstHref);
                }
            } else {
                redisUtil.setString(firstTitel, firstHref);
            }

            for (int i = 0; i < nodes.size(); i++) {
                Selectable selectable = nodes.get(i);
                // 文字元素
                Selectable item = selectable.xpath("//div[@class='txt-box']");
                // 图片元素
                Selectable imgItem = selectable.xpath("//div[@class='img-box']");

                // 数据
                String title = item.xpath("//h3/a/text()").toString();
                String href = item.xpath("//a/@href").toString();
                String source = item.xpath("//a[@class='account']/text()").toString();
                String share = item.xpath("//a/@data-share").toString();
                String imgUrl = "http:" + imgItem.xpath("//a/img/@src").toString();
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
                urlList.add(href);

                NewsDO newsDO = newsCoreManager.buildNewsDO(newId, dataKey, title, 0, href, share, source, href, NewsDO.NEWS_TYPE_HEADLINE, NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                        "", 0, "", date, 0, 1, date, NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE,
                        0, 0);
                list.add(newsDO);

                // 上传图片至阿里云
                String saveImg = aliyunOSSClientUtil.uploadPicture(imgUrl);
//                String saveImg = "";

                NewsImageDO newsImage = new NewsImageDO(IDUtil.getNewID(), newId, srcWidth, srcHeight, NewsImageDO.IMAGE_TYPE_MINI, saveImg, NewsImageDO.STATUS_NORMAL);
                imageList.add(newsImage);
            }

            page.addTargetRequests(urlList);

        } catch (Exception e){
             LOGGER.error("抓取搜狗微信新闻失败：{}", e);
        }

    }

    /**
     * 获取新闻详情
     * @param page
     */
    public void getDetails(Page page) {
        String content = page.getHtml().xpath("//div[@id='js_content']").toString();
//        System.out.println("替换之前" + JSON.toJSONString(content));

        content = schedulerUtils.replaceLabel(content);
        content = otherLabel(content);
//        System.out.println("替换之后" + JSON.toJSONString(content));
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
            weiXinSoGouPipeline.save(list, detailList, imageList);
        }

    }

    /**
     * 替换所有其他链接标签
     * @param content
     * @return String
     */
    public String otherLabel(String content) {
        String reg = "<section[^>]*?>[\\s\\S]*?</section[^>]*?>";
        Pattern p=Pattern.compile(reg);
        Matcher m=p.matcher(content);
        while(m.find()){
            content = m.replaceAll("");
        }
        return content;
    }

    @Override
    public Site getSite() {
        return SITE;
    }
}
