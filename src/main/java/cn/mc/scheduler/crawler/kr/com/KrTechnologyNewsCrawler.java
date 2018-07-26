package cn.mc.scheduler.crawler.kr.com;

import cn.mc.core.constants.CodeConstants;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.assertj.core.util.DateUtil;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 36 氪
 *
 * @auther sin
 * @time 2018/3/28 19:24
 */
@Component
public class KrTechnologyNewsCrawler extends BaseCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(KrTechnologyNewsCrawler.class);

    private Site site = Site.me().setRetryTimes(3).setSleepTime(300);

    private static final String URL = "http://36kr.com/api/search-column/mainsite";

    private static final String NEWS_DETAIL_URL_TEMPLATE = "http://36kr.com/p/%s.html";

    private Map<String, NewsDO> cacheNewsDO = Maps.newHashMap();

    private Map<String, List<NewsImageDO>> cacheNewsImageDO = Maps.newHashMap();

    private List<String> URL_CACHE = Lists.newArrayList();

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private KrTechnologyNewsCrawlerPipeline krTechnologyNewsCrawlerPipeline;

    @Override
    public BaseSpider createCrawler() {

        int size = 1;
        List<Request> requests = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            String url = String.format("%s?per_page=20&page=%s", URL, i + 1);
            URL_CACHE.add(url);
            Request request = new Request(url);
            requests.add(request);
        }

        // create spider
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(requests.toArray(new Request[]{}));
        return spider;
    }

    @Override
    public synchronized void process(Page page) {
        if (URL_CACHE.contains(page.getUrl().toString())) {
            String resultJsonString = page.getRawText();
            JSONObject resultJSONObject = JSON.parseObject(resultJsonString);

            // 处理 api 返回是否正确
            if (CodeConstants.SUCCESS
                    != resultJSONObject.getInteger("code")) {

                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("result code 返回不为 0，" +
                            "数据不抓取! resultJson {} ", resultJsonString);
                }
            }

            if (!resultJSONObject.containsKey("data")) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("data 数据不存在，不可抓取!" +
                            " resultJsonString {}", resultJsonString);
                }
            }

            // 获取数据的 item
            JSONObject dataJSONObject = resultJSONObject.getJSONObject("data");
            JSONArray itemsJSONArray = dataJSONObject.getJSONArray("items");

            for (Object itemObject : itemsJSONArray) {
                JSONObject itemJSONObject   = (JSONObject) itemObject;

                String title = itemJSONObject.getString("title");
                String newsAbstract = itemJSONObject.getString("summary");
                Date createTime = DateUtil.parse(itemJSONObject.getString("published_at"));
                String imageUrl = itemJSONObject.getString("cover");
                String newsSource = itemJSONObject.getString("column_name");
                String id = itemJSONObject.getString("id");
                String newsSourceUrl = String.format(NEWS_DETAIL_URL_TEMPLATE, id);
                String dataKey = getDataKey(newsSourceUrl);

                // 构建 keywords
                String keywords = getKeywords(itemJSONObject);

                long newsId = IDUtil.getNewID();

                // 处理 newsImage
                List<NewsImageDO> newsImageDOList = new ArrayList<>(1);

                int imageHeight = 0;
                int imageWidth = 0;
                NewsImageDO newsImageDO = newsImageCoreManager.buildNewsImageDO(
                        IDUtil.getNewID(),
                        newsId, imageUrl, imageWidth, imageHeight,
                        NewsImageDO.IMAGE_TYPE_LARGE);

                newsImageDOList.add(newsImageDO);

                // 处理 news
                int newsHot = 0;
                String newsUrl = "";
                String shareUrl = "";
                int newsType = NewsDO.NEWS_TYPE_TECHNOLOGY;
                int contentType = NewsDO.CONTENT_TYPE_IMAGE_TEXT;
                int banComment = 0;
                Date displayTime = cn.mc.core.utils.DateUtil.currentDate();
                int videoCount = 0;
                int imageCount = newsImageDOList.size();
                int displayType = handleNewsDisplayType(imageCount);
                int commentCount = 0;
                int sourceCommentCount = 0;

                NewsDO newsDO = newsCoreManager.buildNewsDO(newsId, dataKey, title, newsHot,
                        newsUrl, shareUrl, newsSource, newsSourceUrl, newsType,
                        contentType, keywords, banComment, newsAbstract, displayTime,
                        videoCount, imageCount, createTime, displayType,
                        sourceCommentCount, commentCount);

                // 添加到 cache
                cacheNewsDO.put(dataKey, newsDO);
                cacheNewsImageDO.put(dataKey, newsImageDOList);

                // 添加 detail request
                page.addTargetRequest(new Request(newsSourceUrl));
            }
        } else {
            String url = String.valueOf(page.getUrl());
            String dataKey = getDataKey(url);
            Html html = page.getHtml();

            String htmlText = html.toString();
            String contentRegex = "\"content\"(.*?),\"cover\"";
            String content = getSubUtilSimple(htmlText, contentRegex);

            String summaryRegex = "\"summary\"(.*?),\"content\"";
            String summary = getSubUtilSimple(htmlText, summaryRegex);

            // 获取 content
            if (StringUtils.isEmpty(content)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("文字内容不满足条件，自动过滤! content {}", content);
                }
                return;
            }

            // 获取 newsDO，如果不存在过滤新闻
            if (!cacheNewsDO.containsKey(dataKey)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("根据 dataKey 获取的新闻不存在! {}", dataKey);
                }
                return;
            }

            // 1、截取内容 2、a 标签替换 div
            content = content.substring(2, content.length() -1)
                    .replaceAll("\\\\", "")
                    .replaceAll("<a","<div")
                    .replaceAll("a>","div>");

            // 如果 summary 是空的，则在头部加上这个 内容
            if (!StringUtils.isEmpty(summary)) {
                content = "<p class=\"summary\">"
                        + summary.substring(2, summary.length() -1) + "</p>" + content;
            }

            // 去保存数据
            krTechnologyNewsCrawlerPipeline.saveKrTechnologyNews(
                    dataKey, cacheNewsDO, cacheNewsImageDO, content);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    private String getDataKey(String newsSourceUrl) {
        return EncryptUtil.encrypt(newsSourceUrl, "md5");
    }

    public String getSubUtilSimple(String soap, String regex) {
        Pattern pattern = Pattern.compile(regex);// 匹配的模式
        Matcher m = pattern.matcher(soap);
        while (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private String getKeywords(JSONObject itemJSONObject) {
        String extractionTags = null;
        if (itemJSONObject.containsKey("extraction_tags")) {
            String extractionTag2s = itemJSONObject.getString("extraction_tags");
            if (!StringUtils.isEmpty(extractionTag2s)) {
                extractionTags = extractionTag2s;
            }
        }

        String keywords = "";
        if (!StringUtils.isEmpty(extractionTags)) {
            StringBuffer keywordsBuffer = new StringBuffer();

            JSONArray tagsJSONArray = null;
            try {
                tagsJSONArray = JSON.parseArray(extractionTags);
            } catch (Exception e) {
                LOGGER.error("转换失败! extractionTags {}", extractionTags);
            }

            if (CollectionUtils.isEmpty(tagsJSONArray)) {
                return keywords;
            }

            for (Object keywordObject : JSON.parseArray(extractionTags)) {
                JSONArray keywordJSONArray = (JSONArray) keywordObject;
                String keyword = keywordJSONArray.getString(0);
                keywordsBuffer.append(keyword);
                keywordsBuffer.append(",");
            }

            // 去掉最后一个 ","
            String keywordsBufferString = keywordsBuffer.toString();
            if (keywordsBufferString.length() > 1) {
                keywords = keywordsBufferString.substring(0, keywordsBufferString.length() - 1);
            }
        }

        return keywords;
    }
}
