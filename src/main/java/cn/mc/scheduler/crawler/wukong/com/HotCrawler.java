package cn.mc.scheduler.crawler.wukong.com;

import cn.mc.core.dataObject.*;
import cn.mc.core.manager.*;
import cn.mc.core.utils.CollectionUtil;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.util.SchedulerUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.utils.HttpConstant;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * todo 需要使用队列来抓取，不然机器内存不够就 挂了
 *
 * @auther sin
 * @time 2018/3/10 16:14
 */
@Component
public class HotCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(2000);

    private Logger logger = LoggerFactory.getLogger(HotCrawler.class);

    private static final String URL = "https://www.wukong.com/wenda/wapshare/feed/brow/?concern_id=6300775428692904450&t=1520669614898&max_behot_time=%s&_signature=T80t2xAfFXDiJ3NabkE0vU.NLc";

    private static final Integer ANSWER_MAX_NUMBER = 50;
    /**
     * 悟空: 的 _signature 是个摆设
     *
     * 签名根据：qid 和 offset 两个值
     *
     * 2MYKYBAXgmh1LFTh
     * 2MYKYBAXgmh1LFTh
     */
    private static final String ANSWER_URL = "https://www.wukong.com/m/wapshare/question/loadmore/" +
            "?offset=%s&count=10&qid=%s&t=1520849341498&format=json&_signature=2MYKYBAXgmh1LFTh";

    private Map<String, NewsDO> cacheNewsDO = Maps.newHashMap();
    private Map<String, NewsContentQuestionDO> cacheNewsContentQuestion = Maps.newConcurrentMap();
    private Map<String, List<NewsContentQuestionImageDO>> cacheNewsContentQuestionImage = Maps.newHashMap();
    private Map<String, HotCrawlerVO> cacheHotCrawlerVO = Maps.newHashMap();
    private List<String> REQUEST_URLS = Lists.newArrayList();

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsQuestionAnswerImageCoreManager newsQuestionAnswerImageCoreManager;
    @Autowired
    private NewsContentAnswerCoreManager newsContentAnswerCoreManager;
    @Autowired
    private NewsQuestionAnswerUserCoreManager newsQuestionAnswerUserCoreManager;
    @Autowired
    private NewsContentQuestionCoreManager newsContentQuestionCoreManager;
    @Autowired
    private NewsContentQuestionImageCoreManager newsContentQuestionImageCoreManager;
    @Autowired
    private HotCrawlerPipeline hotCrawlerPipeline;


    @Override
    public Spider createCrawler() {

        // init urls
        List<Request> requestList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {

            String url;
            if (i == 0) {
                url = String.format(URL, System.currentTimeMillis() - 1000);
            } else {
                url = String.format(URL, System.currentTimeMillis() / 3000 - i * 100);
            }
            REQUEST_URLS.add(url);

            Request request = new Request(url);
            setCookies(request);
            setHeaders(request);
            requestList.add(request);
        }

        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(requestList.toArray(new Request[requestList.size()]));
        return spider;
    }

    private void setCookies(Request request) {
        // cookie
        request.addCookie("tt_webid", "6529770235485914637");
        request.addCookie("tt_webid", "6529770235485914637");
        request.addCookie("answer_finalFrom", "https%3A%2F%2Fwww.baidu.com%2Flink");
        request.addCookie("answer_enterFrom", "");
        request.addCookie("cookie_tt_page", "a6a405826c4daf6f746dfdf70265df43");
    }

    private void setHeaders(Request request) {
        // header
        request.addHeader(":authority", "www.wukong.com");
        request.addHeader(":method", "GET");
//        request.addHeader(":path", "/wenda/wapshare/feed/brow/?" +
//                "concern_id=6300775428692904450&t=1520669614898&max_behot_time=1520304208&_signature=2MYKYBAXgmh1LFTh");
        request.addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        request.addHeader("accept-encoding", "gzip, deflate, br");
        request.addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
        request.addHeader("cache-control", "max-age=0");
        request.addHeader("upgrade-insecure-requests", "1");
        request.addHeader("user-agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3 like Mac OS X) AppleWebKit/602.1.50 (KHTML, like Gecko) CriOS/56.0.2924.75 Mobile/14E5239e Safari/602.1");
    }

    @Override
    public synchronized void process(Page page) {
        String requestUrl = page.getUrl().toString();
        JSONObject resultJsonObject = JSON.parseObject(page.getRawText());

        if (REQUEST_URLS.contains(requestUrl)) {
            String loginStatus = String.valueOf(resultJsonObject.get("login_status"));
            if (!loginStatus.equals("0")) {
                return;
            }

            JSONArray dataJsonArray = (JSONArray) resultJsonObject.get("data");
            // 处理首次请求数据
            handleHome(page, dataJsonArray);
        } else {
            int errorNo =  Integer.valueOf(resultJsonObject.get("err_no").toString());
            if (errorNo != 0) {
                return;
            }
            JSONArray answerJsonArray = (JSONArray) resultJsonObject.get("ans_list");
            if (CollectionUtils.isEmpty(answerJsonArray)) {

                // 检查 cache，保存数据
                Map<String, String> urlParamsMap = analyticalUrlParams(requestUrl);
                String dataKey = String.valueOf(urlParamsMap.get("qid"));
                if (!CollectionUtils.isEmpty(cacheHotCrawlerVO)
                        && cacheHotCrawlerVO.containsKey(dataKey)) {

                    // 获取 保存的数据
                    HotCrawlerVO crawlerBO = cacheHotCrawlerVO.get(dataKey);

                    // 去保存
                    hotCrawlerPipeline.save(crawlerBO);
                }
            } else {
                answer(page, requestUrl, answerJsonArray);
            }
        }
    }

    private void answer(Page page, String requestUrl, JSONArray answerJsonArray) {

        // 解析 dataKey 参数，没有则返回
        Map<String, String> urlParamsMap = analyticalUrlParams(requestUrl);
        String dataKey = urlParamsMap.get("qid");
        Integer offset = Integer.valueOf(urlParamsMap.get("offset"));
        if (StringUtils.isEmpty(dataKey)) {
            return;
        }

        // cache 不存在，不处理数据
        if (!cacheNewsDO.containsKey(dataKey))
            return;

        NewsDO newsDO = cacheNewsDO.get(dataKey);
        Long newsId = newsDO.getNewsId();

        // 首次进入 创建一个，并 put 到 cache
        HotCrawlerVO hotCrawlerBO;
        if (!cacheHotCrawlerVO.containsKey(dataKey)) {
            hotCrawlerBO = new HotCrawlerVO();
            hotCrawlerBO.setNewsDO(newsDO);
            hotCrawlerBO.setNewsContentQuestionDO(cacheNewsContentQuestion.get(dataKey));
            hotCrawlerBO.setNewsContentQuestionImageDOList(cacheNewsContentQuestionImage.get(dataKey));
            hotCrawlerBO.setNewsContentAnswerDOList(new ArrayList<>(0));
            hotCrawlerBO.setNewsQuestionAnswerUserDOList(new ArrayList<>(0));
            hotCrawlerBO.setNewsQuestionAnswerImageDOList(new ArrayList<>(0));
            cacheHotCrawlerVO.put(dataKey, hotCrawlerBO);
        } else {
            hotCrawlerBO = cacheHotCrawlerVO.get(dataKey);
        }

        // 一个问题最多同步多少条
        if (hotCrawlerBO.getNewsContentAnswerDOList().size() >= ANSWER_MAX_NUMBER) {
            return;
        }
        for (Object answerObject : answerJsonArray) {
            JSONObject answerJsonObject = (JSONObject) answerObject;

            // 过滤数据
            String content = SchedulerUtils.contentFilter(
                    String.valueOf(answerJsonObject.get("content")), newsDO.getNewsSource(),newsDO.getNewsId());

            if (StringUtils.isEmpty(content))
                return;

            // TODO: 2018/4/28 先暂时过滤掉 “视频” ({!-- PGC_VIDEO)
            String videoFeature = "{!-- PGC_VIDEO";
            int count = org.apache.commons.lang3.StringUtils.countMatches(content, videoFeature);
            if (count > 0) {
                logger.info("问答抓取到 video 内容！自动过滤！{} ", content);
                continue;
            }

            // 摘要信息
            JSONObject abstractJsonObject = (JSONObject) answerJsonObject.get("content_abstract");
            String newsAbstract = noAbstractGoToArticleGetAbstract(
                    String.valueOf(abstractJsonObject.get("text")));
            JSONArray thumbImageJsonArray = (JSONArray) abstractJsonObject.get("thumb_image_list");

            // 回答的图片
            Long newsContentAnswerId = IDUtil.getNewID();
            List<NewsQuestionAnswerImageDO> newsQuestionAnswerImageDOList
                    = new ArrayList<>(thumbImageJsonArray.size());
            for (Object thumbImageObject : thumbImageJsonArray) {
                JSONObject thumbImageJsonObject = (JSONObject) thumbImageObject;
                String imageUrl = String.valueOf(thumbImageJsonObject.get("url"));
                Integer imageHeight = Integer.valueOf(thumbImageJsonObject.get("height").toString());
                Integer imageWidth = Integer.valueOf(thumbImageJsonObject.get("width").toString());
                String imageDataKey = getImageUrlDataKey(imageUrl);
                newsQuestionAnswerImageDOList.add(newsQuestionAnswerImageCoreManager
                        .buildNewsQuestionAnswerImageDO(
                                IDUtil.getNewID(), newsContentAnswerId, imageDataKey,
                                NewsImageDO.IMAGE_TYPE_LARGE,imageUrl,
                                imageHeight, imageWidth));
            }

            // 回答的用户
            Long newsQuestionAnswerUserId = IDUtil.getNewID();
            JSONObject userJsonObject = (JSONObject) answerJsonObject.get("user");
            String nickName = String.valueOf(userJsonObject.get("uname"));
            String avatarUrl = String.valueOf(userJsonObject.get("avatar_url"));
            String answerUserDataKey = String.valueOf(userJsonObject.get("user_id"));

            NewsQuestionAnswerUserDO newsQuestionAnswerUserDO
                    = newsQuestionAnswerUserCoreManager.buildNewsQuestionAnswerUserDO(
                    newsQuestionAnswerUserId, nickName, avatarUrl, answerUserDataKey);

            // 回答的内容
            int contentAnswerImageCount = newsQuestionAnswerImageDOList.size();
            int contentAnswerVideoCount = 0;

            String contentAnswerDataKey = String.valueOf(answerJsonObject.get("ansid"));
            Integer commentCount = 0;
            Integer sourceCommentCount;

            // 设置 sourceCommentCount
            Integer commentCountWithAnswerJsonObject = answerJsonObject.getInteger("comment_count");
            if(commentCountWithAnswerJsonObject == null){
                sourceCommentCount = 0;
            } else {
                sourceCommentCount = commentCountWithAnswerJsonObject;
            }

            NewsContentAnswerDO newsContentAnswerDO = newsContentAnswerCoreManager
                    .buildNewsContentAnswerDO(newsContentAnswerId, newsId,
                            newsQuestionAnswerUserId, contentAnswerDataKey,
                            requestUrl, NewsContentAnswerDO.BROWSER_USER_NO,
                            content, newsAbstract,0,0,
                            commentCount, sourceCommentCount,
                            contentAnswerImageCount, contentAnswerVideoCount, DateUtil.currentDate());

            // 去重.
            Map<String, NewsContentAnswerDO> newsContentAnswerDOMap
                    = CollectionUtil.buildMap(hotCrawlerBO.getNewsContentAnswerDOList(),
                    String.class, NewsContentAnswerDO.class, "dataKey");

            if (!newsContentAnswerDOMap.containsKey(newsContentAnswerDO.getDataKey())) {
                hotCrawlerBO.getNewsContentAnswerDOList().add(newsContentAnswerDO);
                hotCrawlerBO.getNewsQuestionAnswerUserDOList().add(newsQuestionAnswerUserDO);
                hotCrawlerBO.getNewsQuestionAnswerImageDOList().addAll(newsQuestionAnswerImageDOList);
            }

            // add url
            addAnswerUrl(page, offset + 1, dataKey);
        }
    }

    private void handleHome(Page page, JSONArray dataJsonArray) {
        for (Object dataObject : dataJsonArray) {
            JSONObject dataJsonObject = (JSONObject) dataObject;
            JSONObject answerJsonObject = (JSONObject) dataJsonObject.get("answer");
            JSONObject questionJsonObject = (JSONObject) dataJsonObject.get("question");

            String dataKey = String.valueOf(questionJsonObject.get("qid"));
            String title = String.valueOf(questionJsonObject.get("title")).trim();
            JSONObject questionContentJsonObject = (JSONObject) questionJsonObject.get("content");
            String questionContentText = String.valueOf(questionContentJsonObject.get("text"));
            JSONArray questionContentLargeImageJsonArray
                    = (JSONArray) questionContentJsonObject.get("large_image_list");

            // 问题id
            Long newsContentQuestionId = IDUtil.getNewID();
            List<NewsContentQuestionImageDO> newsContentQuestionImageDOList
                    = new ArrayList<>(questionContentLargeImageJsonArray.size());
            for (Object questionContentLargeImageObject : questionContentLargeImageJsonArray) {
                JSONObject questionContentLargeImageJsonObject = (JSONObject) questionContentLargeImageObject;
                String url = String.valueOf(questionContentLargeImageJsonObject.get("url"));
                Integer height = Integer.valueOf(questionContentLargeImageJsonObject.get("height").toString());
                Integer width = Integer.valueOf(questionContentLargeImageJsonObject.get("width").toString());

                NewsContentQuestionImageDO newsContentQuestionImageDO
                        = newsContentQuestionImageCoreManager.buildNewsContentQuestionImageDO(
                        IDUtil.getNewID(), newsContentQuestionId, height, width, NewsImageDO.IMAGE_TYPE_LARGE, url);
                newsContentQuestionImageDOList.add(newsContentQuestionImageDO);
            }
            Date displayTime = new Date(Long.valueOf(dataJsonObject.get("behot_time").toString()) * 1000);
            String content = String.valueOf(answerJsonObject.get("content"));
            JSONObject newsAbstractJsonObject = (JSONObject) answerJsonObject.get("content_abstract");
            String newsAbstract = noAbstractGoToArticleGetAbstract(
                    String.valueOf(newsAbstractJsonObject.get("text")));

            Long newsId = IDUtil.getNewID();
            String newsSourceUrl = page.getUrl().toString();
            int imageCount = 0;
            int videoCount = 0;
            int qaCount = 0;

            Date createTime = new Date(Long.valueOf(
                    questionJsonObject.get("create_time").toString())  * 1000);

            // 新闻
            NewsDO newsDO = newsCoreManager.buildNewsDOWithQa(newsId, 0L,
                    dataKey, title, 0, "",
                    "", NewsDO.DATA_SOURCE_WU_KONG, newsSourceUrl, NewsDO.NEWS_TYPE_QA,
                    NewsDO.CONTENT_TYPE_IMAGE_TEXT, "", 0, newsAbstract, displayTime,
                    videoCount, imageCount, qaCount, createTime,null);

            // 问题信息
            NewsContentQuestionDO newsContentQuestionDO = newsContentQuestionCoreManager
                    .buildNewsContentQuestionDO(newsContentQuestionId, newsId, title, questionContentText);

            // cache
            cacheNewsDO.put(dataKey, newsDO);
            cacheNewsContentQuestion.put(dataKey, newsContentQuestionDO);
            cacheNewsContentQuestionImage.put(dataKey, newsContentQuestionImageDOList);

            // 添加 回答请求
            int firstOffset = 1;
            addAnswerUrl(page, firstOffset ,dataKey);
        }
    }

    private void addAnswerUrl(Page page, int offset, String dataKey) {
        // add request
        Request request = new Request(String.format(ANSWER_URL, offset, dataKey));

        request.addHeader("", ":authority:www.wukong.com");
        request.addHeader("", "method:GET");
        request.addHeader("", String.format("path:/m/wapshare/question/loadmore/?offset=12&count=10" +
                "&qid=%s&t=1520843385599&format=json&_signature=XHuithAdBt7xkfw3hGD601x7oq", dataKey));
        request.addHeader("", "scheme:https");
        request.addHeader("accept", "application/json");
        request.addHeader("accept-encoding", "gzip, deflate, br");
        request.addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
        request.addHeader("cookie", "tt_webid=6529770235485914637; tt_webid=6529770235485914637; " +
                "wendacsrftoken=3369c7ab7c433a99e0f5c29f46b27774");
        request.addHeader("referer", String.format("https://www.wukong.com/question/%s/", dataKey));
        request.addHeader("user-agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3 like Mac OS X) " +
                "AppleWebKit/602.1.50 (KHTML, like Gecko) CriOS/56.0.2924.75 Mobile/14E5239e Safari/602.1");
        request.addHeader("x-requested-with", "XMLHttpRequest");

        request.setMethod(HttpConstant.Method.GET);
        request.addCookie("tt_webid", "6529770235485914637");
        request.addCookie("wendacsrftoken", "3369c7ab7c433a99e0f5c29f46b27774");
        page.addTargetRequest(request);
    }

    private String getImageUrlDataKey(String imageUrl) {
        return EncryptUtil.encrypt(imageUrl, "md5");
    }

    @Override
    public Site getSite() {
        return site;
    }
}
