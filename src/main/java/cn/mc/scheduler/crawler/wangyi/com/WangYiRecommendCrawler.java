package cn.mc.scheduler.crawler.wangyi.com;

import cn.mc.core.constants.CodeConstants;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.util.HtmlNodeUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.util.*;

/**
 * 网易推荐
 *
 * @author sin
 * @time 2018/7/23 10:10
 */
@Component
public class WangYiRecommendCrawler extends BaseCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WangYiRecommendCrawler.class);

    private Site site = Site.me().setRetryTimes(3).setSleepTime(300);

    private static String URL = "https://3g.163.com/touch/nc/api/user/recommend/" +
            "GuessLike/";

    private Map<String, NewsDO> cacheNewsDO = Maps.newHashMap();

    private Map<String, List<NewsImageDO>> cacheNewsImageDO = Maps.newHashMap();

    private List<String> URL_CACHE = Lists.newArrayList();

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private WangYiSportPipeline wangyiSportPipeline;


    @Override
    public Spider createCrawler() {

        ///0-20.html
        int pageIndex = new Random().nextInt(150) + 1;
        String url = String.format("%s/%s-10-10-10.do?__r" +
                "nd=30375151&callback=recommendList", URL, pageIndex);

        Request request = new Request(url);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);

        URL_CACHE.add(url);
        return spider;
    }

    @Override
    public synchronized void process(Page page) {
        if (URL_CACHE.contains(page.getUrl().toString())) {

            // 提取 jsonP，转换为 json 数据
            String jsonPText = page.getRawText();
            String jsonDataString = jsonPText.substring(
                    jsonPText.indexOf("recommendList") + 14, jsonPText.length() - 1);

            JSONObject jsonData = JSON.parseObject(jsonDataString);

            // 判断返回数据是否可用
            if (CodeConstants.HTTP_SUCCESS_200 != jsonData.getInteger("code")) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("返回的数据不可用! {}", jsonPText);
                }
                return;
            }

            if (!jsonData.containsKey("list")) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("list 列表不存在没有数据抓取!");
                }
                return;
            }

            JSONArray listArray = jsonData.getJSONArray("list");
            for (Object itemObject : listArray) {
                JSONObject itemJSONObject = (JSONObject) itemObject;

                Long newsId = IDUtil.getNewID();
                String docId = itemJSONObject.getString("docid");
                String title = itemJSONObject.getString("title");
                String newsSourceUrl = itemJSONObject.getString("link");
                String dataKey = getDataKey(newsSourceUrl);
                String newsSource = itemJSONObject.getString("source");
                String keywords = itemJSONObject.getString("unlikeReason")
                        .replaceAll("/\\d", "");

                String newsAbstract = itemJSONObject.getString("digest");
                if (StringUtils.isEmpty(newsAbstract) || newsAbstract.equals("null")) {
                    newsAbstract = title;
                }

                Date createTime = DateUtil.parse(
                        itemJSONObject.getString("ptime"), "yyyy-MM-dd HH:mm");
                JSONArray picInfoArray = itemJSONObject.getJSONArray("picInfo");

                // 处理新闻图片
                List<NewsImageDO> newsImageDOList = new ArrayList<>(picInfoArray.size());
                for (Object picInfo : picInfoArray) {
                    JSONObject jsonObject = (JSONObject) picInfo;
                    String imageUrl = jsonObject.getString("url");

                    Long newsImageId = IDUtil.getNewID();
                    int imageHeight = 0;
                    int imageWidth = 0;
                    NewsImageDO newsImageDO = newsImageCoreManager.buildNewsImageDO(
                            newsImageId, newsId, imageUrl, imageWidth, imageHeight,
                            NewsImageDO.IMAGE_TYPE_MINI);
                    newsImageDOList.add(newsImageDO);
                }

                // 列表没有图片的新闻不了
                if (CollectionUtils.isEmpty(newsImageDOList)) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("列表没有图片，自动过滤!");
                    }
                    return;
                }

                int newsHot = 0;
                String newsUrl = "";
                String shareUrl = "";
                int newsType = NewsDO.NEWS_TYPE_HEADLINE;
                int contentType = NewsDO.CONTENT_TYPE_IMAGE_TEXT;
                int banComment = 0;
                Date displayTime = DateUtil.currentDate();
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

                // 添加缓存
                cacheNewsDO.put(dataKey, newsDO);
                cacheNewsImageDO.put(dataKey, newsImageDOList);

                // 添加 info 请求地址
                Request request = new Request(newsSourceUrl);
                page.addTargetRequest(request);
            }

        } else {
            String url = String.valueOf(page.getUrl());
            String dataKey = getDataKey(url);
            Html html = page.getHtml();

            // 抓取文字内容
            Selectable contentNode = html.xpath("//div[@class='content']");
            if (contentNode == null) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("没有内容，自动过滤! html {}", html);
                }
                return;
            }

            // 处理 content cdn 图片
            String imageSourceAttr = "data-src";
            String imageTargetAttr = "src";
            List<Selectable> imageNodes = contentNode.xpath("//img").nodes();
            HtmlNodeUtil.handleCdnWithRemoveSourceAttr(
                    imageNodes, imageSourceAttr, imageTargetAttr);

            // 处理 a link，cdn 路径
            List<Selectable> aNodes = contentNode.xpath("//a").nodes();
            HtmlNodeUtil.handleCdnUrl(aNodes, "href");

            // 获取 content
            String content = contentNode.toString();
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

            // 去保存
            wangyiSportPipeline.saveNews(dataKey, cacheNewsDO, cacheNewsImageDO, content);
        }
    }


    private String getDataKey(String newsSourceUrl) {
        return EncryptUtil.encrypt(newsSourceUrl, "md5");
    }

    @Override
    public Site getSite() {
        return site;
    }
}
