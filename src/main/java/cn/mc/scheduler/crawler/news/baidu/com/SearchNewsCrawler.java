package cn.mc.scheduler.crawler.news.baidu.com;

import cn.mc.core.dataObject.*;
import cn.mc.core.manager.NewsContentArticleCoreManager;
import cn.mc.core.manager.NewsContentPictureCoreManager;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.assertj.core.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.util.*;

/**
 * 百度搜索 下的新闻爬虫
 *
 * @auther sin
 * @time 2018/3/13 14:29
 */
@Component
public class SearchNewsCrawler extends BaseCrawler {

    /**
     * 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
     */
    private Site site = Site.me().setRetryTimes(3).setSleepTime(300);

    protected Logger logger = LoggerFactory.getLogger(SearchNewsCrawler.class);

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsContentArticleCoreManager newsContentArticleCoreManager;
    @Autowired
    private NewsContentPictureCoreManager newsContentPictureCoreManager;
    @Autowired
    private SearchNewsCrawlerPipeline searchNewsCrawlerPipeline;

    private final String URL = "https://feed.baidu.com/feed/api/wise/feedlist?sid=124910_1237" +
            "61_123731_120139_118888_118859_118852_118836_118791_107312_122035_117328_117432_" +
            "124602_122788_124752_124619_123985_124559_124110_124614_124525_124937_124770_123980" +
            "_124030_124768_110085_124308_123289&from=0&pu=sz%25401320_2001%252Cta%2540iphone" +
            "_1_11.0_3_604&qid=2121708290&clickDownload=0&tabId=1&page=1&sessionId=15321513217155" +
            "&crids=&startlistnum=64&rfs=7&loadeditems=newsThree%2CecomAds%2CtypeLargePics" +
            "&callback=jsonp3";

    private Map<String, SearchNewsOneVO> cacheData = Maps.newHashMap();

    private List<String> REQUEST_URLS = Lists.newArrayList();

    @Override
    public BaseSpider createCrawler() {
        // init request
        int requestSize = 4;
        List<Request> requests = new ArrayList<>();

        for (int i = 0; i < requestSize; i++) {
            int randomMinute = new Random().nextInt(30);
            long currentTimeMillis = System.currentTimeMillis() - 60000 * randomMinute;
            String ssId = "eddd4368657269736853696e6365c437";
            String url = String.format("%s&_=%s&ssid=%s", URL, currentTimeMillis, ssId);

            REQUEST_URLS.add(url);
            Request request = new Request(url);
            addHeaders(request);
            requests.add(request);
        }

        BaseSpider mySpider = new BaseSpider(this);
        mySpider.addRequest(requests.toArray(new Request[requestSize]));
        return mySpider;
    }

    @Override
    public void process(Page page) {
        String requestUrl = page.getUrl().toString();

        if (REQUEST_URLS.contains(requestUrl)) {
            String jsonP = page.getRawText()
                    .replaceFirst("jsonp3\\(", "");
            String resultJsonStr = jsonP.substring(0, jsonP.length() - 1);
            JSONObject resultJsonObject = JSON.parseObject(resultJsonStr);
            String errno = String.valueOf(resultJsonObject.get("errno"));
            if (!errno.equals("0")) {
                return;
            }

            JSONObject resultJsonObject1 = (JSONObject) resultJsonObject.get("result");

            JSONObject resultTabJsonObject = (JSONObject) resultJsonObject1.get("tab");
            String htmlData = String.valueOf(resultTabJsonObject.get("data"));
            Html html = new Html(htmlData);
            List<Selectable> newsItemNodes = html.xpath("//div[@data-click-grey").nodes();

            for (Selectable newsItemNode : newsItemNodes) {

                // 是否是视频
                NewsDisplayBO newsDisplayBO = getNewsContentType(newsItemNode);
                Integer contentType = newsDisplayBO.getContentType();
                Integer displayType = newsDisplayBO.getDisplayType();
                if (NewsDO.CONTENT_TYPE_VIDEO.equals(contentType)) {
//                     handleVideo(page, newsItemNode);
                } else if (NewsDO.CONTENT_TYPE_LARGE_PIC.equals(contentType)
                        && NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE.equals(displayType)) {
                    handleContentLargePicNews(page, newsItemNode, newsDisplayBO);
                }  else if (NewsDO.CONTENT_TYPE_IMAGE_TEXT.equals(contentType)) {
                    handleContentTypeImageText(page, newsItemNode, newsDisplayBO);
                }
            }
        } else {

            // 处理网页
            Map<String, String> pageTowUrlParams = analyticalUrlParams(requestUrl);
            String dataKey = pageTowUrlParams.get("dataKey");

            if (!cacheData.containsKey(dataKey)) {
                return;
            }

            SearchNewsOneVO searchNewsOneVO = cacheData.get(dataKey);

            // 新闻类型不存在 跳过
            if (searchNewsOneVO.getNewsDisplayBO() == null) {
                return;
            }

            // 获取 新闻显示 bo
            NewsDisplayBO newsDisplayBO = searchNewsOneVO.getNewsDisplayBO();
            Integer contentType = newsDisplayBO.getContentType();
            Integer displayType = newsDisplayBO.getDisplayType();

            if (NewsDO.CONTENT_TYPE_LARGE_PIC.equals(contentType)
                    && NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE.equals(displayType)) {
                // 大图新闻 - 大图显示
                handleLargePicNews(searchNewsOneVO, page);
            } else if (NewsDO.CONTENT_TYPE_IMAGE_TEXT.equals(contentType)) {
                // 图文新闻
                handleNewsDetailPage(dataKey, page);
            }
        }
    }

    private void handleLargePicNews(SearchNewsOneVO searchNewsOneVO, Page page) {

        // 获取所有的 大图节点
        Selectable swipeItemsNode = page.getHtml().xpath(
                "//div[@class='enlargeimage-swipe-div']");

        List<NewsContentPictureDO> newsContentPictureDOList
                = new ArrayList<>(swipeItemsNode.nodes().size());
        for (Selectable swipeItemNode : swipeItemsNode.nodes()) {
            Selectable swipeImage = swipeItemNode.xpath("//div[@class='mip-img-container']/img");
            if (swipeImage.nodes().size() <= 0) {
                logger.error("图片获取不到，自动过滤! {}", swipeItemNode.toString());
                continue;
            }

            // 大图新闻 - 图片是懒加载的，所以没看的图片属性在 data-original 里面
            String swipeImageUrl = swipeImage.xpath("//img/@src").toString();
            if (StringUtils.isEmpty(swipeImageUrl)) {
                swipeImageUrl = swipeImage.xpath("//img/@data-original").toString();
            }

            if (StringUtils.isEmpty(swipeImageUrl)) {
                logger.error("图片获取不到，自动过滤! {}", swipeItemNode.toString());
                continue;
            }

            // 获取图片 高宽
            Map<String, String> swipeImageParams = analyticalUrlParams(swipeImageUrl);
            if (StringUtils.isEmpty(swipeImageParams.get("w"))) {
                logger.error("图片宽度获取不到，自动过滤! {}", swipeItemNode.toString());
                continue;
            }
            Integer swipeImageWidth = Integer.valueOf(swipeImageParams.get("w"));
            Integer swipeImageHeight = Integer.valueOf(swipeImageParams.get("h"));

            String content = swipeItemNode.xpath(
                    "//p[@class='enlargeimage-swipe-p2']/html()").toString();

            NewsDO newsDO = searchNewsOneVO.getNewsDO();
            NewsContentPictureDO newsContentPictureDO
                    = newsContentPictureCoreManager.buildNewsContentPictureDO(
                    IDUtil.getNewID(), newsDO.getNewsId(),
                    swipeImageUrl, swipeImageWidth, swipeImageHeight,
                    newsContentPictureDOList.size(), content);

            newsContentPictureDOList.add(newsContentPictureDO);
        }

        // 保存新闻
        NewsDO newsDO = searchNewsOneVO.getNewsDO();
        String sourceUrl = page.getUrl().toString();
        newsDO.setNewsSourceUrl(sourceUrl);

        // 保存
        searchNewsCrawlerPipeline.savePicture(newsDO, searchNewsOneVO, newsContentPictureDOList);
    }


    private void handleNewsDetailPage(String dataKey, Page page) {
        SearchNewsOneVO searchNewsOneVO = cacheData.get(dataKey);
        String content = page.getHtml().xpath("//div[@class='mainContent']/html()").toString();

        if (StringUtils.isEmpty(content)) {
            return;
        }

        // 处理 newsDO 摘要信息
        NewsDO newsDO = searchNewsOneVO.getNewsDO();
        newsDO.setNewsAbstract(noAbstractGoToArticleGetAbstract(content));

        // 文字信息
        NewsContentArticleDO newsContentArticleDO = newsContentArticleCoreManager
                .buildNewsContentArticleDO(IDUtil.getNewID(), newsDO.getNewsId(), content);

        // 保存
        searchNewsCrawlerPipeline.saveDetailPage(
                dataKey, newsDO, searchNewsOneVO, newsContentArticleDO);
    }

    /**
     * 处理内容 - 大图新闻
     *
     * @param page
     * @param newsItemNode
     * @param newsDisplayBO
     */
    private void handleContentLargePicNews(Page page,
                                       Selectable newsItemNode,
                                       NewsDisplayBO newsDisplayBO) {

        // 新闻 title
        String title = getPageOneTitle(newsItemNode);
        if (null == title) {
            return;
        }

        // 数据来源
        String newsSource = getPageOneNewsSource(newsItemNode);
        if (null == newsSource) {
            return;
        }

        // img 地址
        String picImageUrl = newsItemNode.xpath(
                "//div[@class='rn-large-pic-content rn-pos-relative']/img/@src").toString();

        Map<String, String> picImageParams = analyticalUrlParams(picImageUrl);
        if (StringUtils.isEmpty(picImageUrl)) {
            return;
        }

        // 页面显示的时间 03:23 (月/日)
        String createTime = newsItemNode.xpath("//span[@class='rn-domainTime']").toString();
        if (StringUtils.isEmpty(createTime)) {
            return;
        }

        // 抓取下一个页面的 url
        String pageTowUrl = newsItemNode.xpath("//a/@href").toString();

        // 获取 dataKey
        String dataKey = getPageOneDataKey(newsItemNode);

        // 添加 详情页请求
        Request request = new Request(String.format("%s&dataKey=%s", pageTowUrl, dataKey));
        page.addTargetRequest(request);

        // build 新闻
        Long newsId = IDUtil.getNewID();
        int imageCount = 1;
        NewsDO newsDO = newsCoreManager.buildNewsDO(newsId, dataKey, title,
                0, "", "", newsSource,
                "", NewsDO.NEWS_TYPE_HEADLINE,
                newsDisplayBO.getContentType(), "",
                0, title, DateUtil.currentDate(),
                0, imageCount, DateUtil.currentDate(),
                newsDisplayBO.getDisplayType(),
                0, 0);

        // build 封面图
        String imageSize = picImageParams.get("size");
        if (StringUtils.isEmpty(imageSize))
            throw new RuntimeException("imageSize 不存在，自动过滤!");

        String[] imageSizeArray = imageSize
                .replace("f", "").split("_");

        Integer picImageWidth = Integer.valueOf(imageSizeArray[0]);
        Integer picImageHeight = Integer.valueOf(imageSizeArray[1]);
        NewsImageDO newsImageDO = newsImageCoreManager.buildNewsImageDO(
                IDUtil.getNewID(), newsId,
                picImageUrl, picImageWidth,
                picImageHeight, NewsImageDO.IMAGE_TYPE_LARGE);

        // 创建 vo
        SearchNewsOneVO searchNewsOneVO = new SearchNewsOneVO();
        searchNewsOneVO.setNewsDO(newsDO);
        searchNewsOneVO.setNewsImageDOList(Lists.newArrayList(newsImageDO));
        searchNewsOneVO.setNewsDisplayBO(newsDisplayBO);

        // 缓存 dataKey，和 newsDisplayBO， 用于区分详情页类型区分
        cacheData.put(dataKey, searchNewsOneVO);
    }

    private void handleContentTypeImageText(Page page,
                                            Selectable newsItemNode,
                                            NewsDisplayBO newsDisplayBO) {
        String title = getPageOneTitle(newsItemNode);
        if (null == title) {
            return;
        }

        String newsSource = getPageOneNewsSource(newsItemNode);
        if (null == newsSource) {
            return;
        }

        List<Selectable> threeImageNodes = newsItemNode.xpath("//img").nodes();
        if (CollectionUtils.isEmpty(threeImageNodes)) {
            return;
        }

        // displayTime
        Date displayTime = null;
        String dataKey = getPageOneDataKey(newsItemNode);
        String htmlTime = newsItemNode.xpath("//span[@class='rn-domainTime']/text()").toString();
        if (StringUtils.isEmpty(htmlTime)) {
            displayTime = DateUtil.currentDate();
        }

        if (displayTime == null) {
            if (htmlTime.indexOf(":") != -1) {
                // 时间
                String completionTime = DateUtil.format(DateUtil.currentDate(), "yyyy-MM-dd");
                displayTime = DateUtil.parse(completionTime + " " + htmlTime, "yyyy-MM-dd HH:mm");
            } else {
                // 月日
                String completionTime = DateUtil.format(DateUtil.currentDate(), "yyyy");
                displayTime = DateUtil.parse(completionTime + "-" + htmlTime, "yyyy-MM-dd");
            }
        }

        long newsId = IDUtil.getNewID();
        String newsAbstract = noAbstractGoToArticleGetAbstract(title);
        Date createTime = displayTime;
        int videoCount = 0;
        int imageCount = threeImageNodes.size();
        List<NewsImageDO> newsImageDOList = new ArrayList<>(threeImageNodes.size());

        for (Selectable threeImageNode : threeImageNodes) {
            String imageUrl = threeImageNode.xpath("img/@src").get();
            Map<String, String> imageUrlParams = analyticalUrlParams(imageUrl);

            Integer imageWidth = 0;
            Integer imageHeight = 0;
            if (imageUrlParams.get("w") != null) {
                imageWidth = Integer.valueOf(imageUrlParams.get("w"));
            }

            if (imageUrlParams.get("h") != null) {
                imageHeight = Integer.valueOf(imageUrlParams.get("h"));
            }

            newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                    IDUtil.getNewID(), newsId, imageUrl, imageWidth,
                    imageHeight, NewsImageDO.IMAGE_TYPE_MINI));
        }

        // yy
        Integer contentType;
        if (newsImageDOList.size() >= 1) {
            contentType = NewsDO.CONTENT_TYPE_IMAGE_TEXT;
        } else {
            contentType = NewsDO.CONTENT_TYPE_TEXT;
        }

        // 显示类型
        Integer displayType = handleNewsDisplayType(newsImageDOList.size());
        // 抓取下一个页面的 url
        String pageTowUrl = newsItemNode.xpath("//a/@href").toString();
        NewsDO newsDO = newsCoreManager.buildNewsDO(newsId, dataKey, title, 0,
                "", "", newsSource, pageTowUrl, NewsDO.NEWS_TYPE_HEADLINE,
                contentType,"", 0, newsAbstract, displayTime,
                videoCount, imageCount, createTime, displayType, 0, 0);

        // cache
        SearchNewsOneVO searchNewsOneVO = new SearchNewsOneVO();
        searchNewsOneVO.setNewsDO(newsDO);
        searchNewsOneVO.setContentType(NewsDO.CONTENT_TYPE_IMAGE_TEXT);
        searchNewsOneVO.setNewsImageDOList(newsImageDOList);
        // 添加 newsDisplayBO、用于详情页信息区分
        searchNewsOneVO.setNewsDisplayBO(newsDisplayBO);
        cacheData.put(dataKey, searchNewsOneVO);

        // 添加详情页请求地址
        Request request = new Request(String.format("%s&dataKey=%s", pageTowUrl, dataKey));
        page.addTargetRequest(request);
    }

    private void handleVideo(Page page, Selectable newsItemNode) {
        String dataKey = getPageOneDataKey(newsItemNode);
        String title = getPageOneTitle(newsItemNode);
        if (null == title) {
            return;
        }

        String newsSource = getPageOneNewsSource(newsItemNode);
        if (null == newsSource) {
            return;
        }

        // displayTime
        String time = newsItemNode.xpath("//span[@class='rn-domainTime']/text()").toString();
        Date displayTime;
        if (StringUtils.isEmpty(time)) {
            displayTime = DateUtil.currentDate();
        } else {
            String currentYear = DateUtil.format(DateUtil.currentDate(), "yyyy");
            displayTime = DateUtil.parse(currentYear + "-" + time, "yyyy-MM-dd");
        }

        String videoImage = newsItemNode.xpath("//div[@class='rn-large-pic-content']/img/@src").toString();
        String videoUrl = getPageOneUrl(newsItemNode);

        Long newsId = IDUtil.getNewID();
        Date createTime = displayTime;
        int videoCount = 0;
        String newsAbstract = noAbstractGoToArticleGetAbstract(title);

        // 显示类型
        Integer displayType = handleVideoDisplayType();
        NewsDO newsDO = newsCoreManager.buildNewsDO(newsId, dataKey, title, 0,
                "", "",
                newsSource, videoUrl, NewsDO.NEWS_TYPE_VIDEO, NewsDO.CONTENT_TYPE_VIDEO,
                "", 0, newsAbstract, displayTime, videoCount, 0,
                createTime, displayType, 0, 0);

        NewsContentVideoDO newsContentVideoDO = new NewsContentVideoDO();
        newsContentVideoDO.setNewsId(newsId);
        newsContentVideoDO.setNewsContentVideoId(IDUtil.getNewID());
        newsContentVideoDO.setVideoImage(videoImage);
        newsContentVideoDO.setVideoIntroduce("");

//        cacheData.put(dataKey, )
//        SearchNewsOneVO searchNewsOneVO = new SearchNewsOneVO();
//        newsContentVideoCoreManager.buildNewsContentVideoDO(
//                IDUtil.getNewID(), newsId, videoUrl, "", 0,
//                NewsContentVideoDO.FORMAT_MP4, "", videoImage, )
//        searchNewsOneVO.setNewsDO(newsDO);
//        searchNewsOneVO.setNewsContentVideoDO(newsContentVideoDO);
//        searchNewsOneVO.setVideo(true);
//        searchNewsOneVO.setNewsImageDOList();
//
//        Request request = new Request(videoUrl);
//        page.addTargetRequest(request);
    }

    private String getPageOneUrl(Selectable newsItemNode) {
        return newsItemNode.xpath("//a/@href").toString();
    }

    private String getPageOneNewsSource(Selectable newsItemNode) {
        return newsItemNode.xpath("//span[@class='rn-domainName']/text()").toString();
    }

    private String getPageOneTitle(Selectable newsItemNode) {
        String title = newsItemNode.xpath("//div[@class='rn-h2']/text()").toString();
        if (null == title) {
            title = newsItemNode.xpath("//div[@class='rn-h1']/text()").toString();
        }
        return title;
    }

    private String getPageOneDataKey(Selectable newsItemNode) {
        return encrypt(getPageOneTitle(newsItemNode));
    }

    private NewsDisplayBO getNewsContentType(Selectable newsItemNode) {

        // 多图新闻 - 大图
        Integer largePicSize = newsItemNode.xpath(
                "//div[@class='rec-item rn-typeLargePics']").nodes().size();

        if (largePicSize > 0) {
            return new NewsDisplayBO(
                    NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE,
                    NewsDO.CONTENT_TYPE_LARGE_PIC);
        }

//        List<Selectable> newsPic = newsItemNode.xpath(
//                "//div[@class='rn-wise-icon-pics']/span/text()").nodes();

//        if (newsPic.size() > 0) {
//            return new ContentTypeAndDisplayTypeBO(
//                    NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE,
//                    NewsDO.CONTENT_TYPE_LARGE_PIC);
//            return NewsDO.CONTENT_TYPE_LARGE_PIC;
//        }

        // 视频
        List<Selectable> newsVideo = newsItemNode.xpath(
                "//span[@class='rn-wise-large-video-play-icon']").nodes();

        if (newsVideo.size() > 0) {
            return new NewsDisplayBO(
                    NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE,
                    NewsDO.CONTENT_TYPE_VIDEO);
        }

        // 图文
        return new NewsDisplayBO(
                NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE,
                NewsDO.CONTENT_TYPE_IMAGE_TEXT);
    }



    @Override
    public Site getSite() {
        return site;
    }

    public void addHeaders(Request request) {
        request.addHeader(":authority", "feed.baidu.com");
        request.addHeader(":method", "GET");
        request.addHeader(":path", "/feed/api/wise/feedlist?i=1&sid=122602_122331_122156_114178_122320_120185_118888_118865_118844_118832_118795_122187_107316_122247_121861_117433_121666_122295_122138_120852_122480_119327_116407_110085_121732_122303&ssid=686b7368756e6875615f646f6e679d0c&from=0&pu=sz%25401320_2001%252Cta%2540iphone_1_9.1_3_601&qid=3493334366&tabId=1&page=1&sessionId=15203374933493&crids=&startlistnum=8&rfs=7&loadeditems=newsThree%2CtypeSmallPics%2CtypeLargePics%2CecomAds%2C&_=1520337509367&callback=jsonp3");
        request.addHeader(":scheme", "https");
        request.addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        request.addHeader("accept-encoding", "gzip, deflate, br");
        request.addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
        request.addHeader("cache-control", "no-cache");
        request.addHeader("cookie", "PSTM=1519696502; __cfduid=db148f82394e6677e80c1534dc07cd2901520219332; BAIDUID=9733969D4BEBE95619F2E78866F37E63:FG=1; BDUSS=FlYlRWR0Y4UkFIaTAzMGJCeDFlc1BIUWY3clJpSXJYckx1ZkVjS0tSMzBDbVJiQVFBQUFBJCQAAAAAAAAAAAEAAADt3cQ3Q2hlcmlzaFNpbmNlAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAPR9PFv0fTxbMW; BIDUPSID=A7C60D2C1A763B7458FA459BECB09A4B; BDORZ=B490B5EBF6F3CD402E515D22BCDA1598; MCITY=-%3A; PSINO=6; BDRCVFR[feWj1Vr5u3D]=I67x6TjHwwYf0; locale=zh; H_PS_PSSID=26522_1446_26458_21093_20697_20929; __bsi=6837479800336147619_h2_5_R_R_19_0303_c02f_Y");
        request.addHeader("pragma", "no-cache");
        request.addHeader("upgrade-insecure-requests", "1");
        request.addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
    }
}
