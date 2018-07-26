package cn.mc.scheduler.crawler.qiushibaike.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsSectionUserDO;
import cn.mc.core.manager.NewsContentArticleCoreManager;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsSectionUserCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.crawler.BaseCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Selectable;

import java.util.*;

/**
 * @auther sin
 * @time 2018/3/7 13:28
 */
@Component
@Deprecated
public class QiuShiCrawler extends BaseCrawler {

    /**
     * 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
     */
    private Site site = Site.me().setRetryTimes(3).setSleepTime(2000);
    /**
     * 服务地址
     */
    private static final String SERVER_URL = "https://www.qiushibaike.com";

    private static final String CRAWLER_URL = "https://www.qiushibaike.com/8hr/page/%s/";

    private static Map<String, NewsDO> cacheNewsData = Collections.EMPTY_MAP;
    private static Map<String, NewsSectionUserDO> cacheNewsSectionData = Collections.EMPTY_MAP;

    public static final String RESULT_ITEM_NEWS_DO_KEY = "newsDO";
    public static final String RESULT_ITEM_NEWS_CONTENT_TEXT_DO_KEY = "newsContentTextDO";
    public static final String RESULT_ITEM_NEWS_SECTION_USER_KEY = "newsSectionUser";

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private QiuShiPipeline qiuShiPipeline;
    @Autowired
    private NewsContentArticleCoreManager newsContentArticleCoreManager;
    @Autowired
    private NewsSectionUserCoreManager newsSectionUserCoreManager;

    @Override
    public Spider createCrawler() {

        // init
        cacheNewsData = new LinkedHashMap<>(0);
        cacheNewsSectionData = new LinkedHashMap<>(0);

        int size = 5;
        String[] urls = new String[size];
        for (int i = 0; i < size; i++) {
            urls[i] = String.format(CRAWLER_URL, i + 1);
        }
        return Spider.create(this)
                .addUrl(urls)
                .addPipeline(qiuShiPipeline);
    }

    @Override
    public synchronized void process(Page page) {
        String contentHtml = page.getHtml().xpath("//div[@class='content-text']/html()").toString();
        if (contentHtml != null) {
            contentHtml = contentHtml.replaceAll("\r\n", "")
                    .replaceAll("\n", "");
        }
        List<Selectable> htmlNodes = page.getHtml().xpath("//article[@class='item']").nodes();
        String serverUrl = SERVER_URL;
        if (!CollectionUtils.isEmpty(htmlNodes)) {
            // 列表
            pageList(page, serverUrl, htmlNodes);
        }
        else if (!StringUtils.isEmpty(contentHtml)) {
            // 详细内容
            pageInfo(page, contentHtml);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    private void pageInfo(Page page, String contentHtml) {

        String author = page.getHtml()
                .xpath("//span[@class='touch-user-name-a']/text()")
                .toString();

        if (StringUtils.isEmpty(author)) {
            return;
        }

        String hashAuthor = getAuthorHashKey(author);
        if (!cacheNewsData.containsKey(hashAuthor)) {
            return;
        }

        // 补全 news 信息
        String newsUrl = "";
        String shareUrl = "";
        String title = noTitleGoToArticleGetTitle(contentHtml);
        String newsAbstract = noAbstractGoToArticleGetAbstract(contentHtml);

        if (StringUtils.isEmpty(title)
                || StringUtils.isEmpty(newsAbstract)) {
            return;
        }
        String sourceCommentCount = page.getHtml()
                .xpath("//span[@class='touch-user-name-a']/text()")
                .toString();
        // 补全信息
        String newsSourceUrl = page.getUrl().toString();
        NewsDO newsDO = cacheNewsData.get(hashAuthor);
        newsDO.setDataKey(getContentTextHashKey(contentHtml));
        newsDO.setNewsUrl(newsUrl);
        newsDO.setShareUrl(shareUrl);
        newsDO.setNewsSourceUrl(newsSourceUrl);

        newsDO.setTitle(title);
        newsDO.setNewsAbstract(newsAbstract);
        newsDO.setDisplayType(handleNewsDisplayType(0));

        // build 文章信息
        NewsContentArticleDO contentArticleDO = newsContentArticleCoreManager
                .buildNewsContentArticleDO(IDUtil.getNewID(), newsDO.getNewsId(), contentHtml);

        // 用户信息
        NewsSectionUserDO newsSectionUserDO = cacheNewsSectionData.get(hashAuthor);

        // 放入 page
        page.putField(RESULT_ITEM_NEWS_DO_KEY, newsDO);
        page.putField(RESULT_ITEM_NEWS_CONTENT_TEXT_DO_KEY, contentArticleDO);
        page.putField(RESULT_ITEM_NEWS_SECTION_USER_KEY, newsSectionUserDO);

        // 打开保存开关
        setNeedSaveWithYes(page);
    }

    private void pageList(Page page, String serverUrl, List<Selectable> htmlNodes) {
        for (Selectable selectable : htmlNodes) {
             String pageInfoUrl = selectable
                    .xpath("//article[@class='item']/a[@class='text']/@href")
                    .toString();

            if (!checkedPageInfoUrl(pageInfoUrl)) {
                continue;
            }

            String url;
            if (!prefixWithHttp(pageInfoUrl)) {
                url = String.format("%s%s", serverUrl, pageInfoUrl);
            }
            else {
                url = pageInfoUrl;
            }

            // 部分三：从页面发现后续的url地址来抓取
            page.addTargetRequests(Arrays.asList(url));
            //获取评论数量
            List<String> commentList= selectable
                    .xpath("//*[@class='box']/i[@class='num']/text()").all();
            Integer sourceCommentCount=0;
            if(null!=commentList && commentList.size()>2){
                sourceCommentCount=Integer.parseInt(commentList.get(3));
            }
            // cache 抓取详细信息时候补全信息
            Date createTime = DateUtil.currentDate();
            Long newsId = IDUtil.getNewID();
            NewsDO newsDO = newsCoreManager.buildNewsDO(
                    newsId, null, null, 0,
                    "", "",
                    NewsDO.DATA_SOURCE_QIU_SHI, "",
                    NewsDO.NEWS_TYPE_SECTION,
                    NewsDO.CONTENT_TYPE_IMAGE_TEXT, "", 0,
                    "", new Date(), 0, 0, createTime,
                    null, sourceCommentCount, 0);

            // 头像信息
            String author = selectable
                    .xpath("//article[@class='item']/header/a[@class='username']/text()")
                    .toString();
            String authorImage = getAuthorImage(selectable);
            NewsSectionUserDO newsSectionUserDO = newsSectionUserCoreManager
                    .buildNewsSectionUserDO(IDUtil.getNewID(), newsId,
                            author, authorImage, encrypt(authorImage));

            String pageKey = getAuthorHashKey(author);
            // cache
            cacheNewsData.put(pageKey, newsDO);
            cacheNewsSectionData.put(pageKey, newsSectionUserDO);
        }
    }

    private boolean prefixWithHttp(String pageInfoUrl) {
        return pageInfoUrl.matches("^[http|https].*");
    }

    private String getAuthorHashKey(String author) {
        return EncryptUtil.encrypt(author, "md5");
    }

    private String getContentTextHashKey(String contentText) {
        return EncryptUtil.encrypt(contentText, "md5");
    }

    private String getAuthorImage(Selectable selectable) {
        String authorImageStyle = selectable.xpath("//header/a[@class='avatar']/@style").toString();
        String authorImage = authorImageStyle.substring(
                authorImageStyle.indexOf("(") + 1,
                authorImageStyle.indexOf(")")
        );
        if (authorImage.indexOf("//") != -1) {
            authorImage = authorImage.replaceFirst("//", "");
        }
        if (authorImage.indexOf("http") == -1) {
            authorImage = "http://" + authorImage;
        }
        return authorImage;
    }

    private boolean checkedPageInfoUrl(String pageInfoUrl) {
        if (pageInfoUrl.matches("javascript.*"))
            return false;
        return true;
    }
}
