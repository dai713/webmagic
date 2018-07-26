package cn.mc.scheduler.crawler.wangyi.com;

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
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Selectable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * 网易 - 娱乐
 *
 * @auther sin
 * @time 2018/3/8 15:42
 */
@Component
public class WangYiEntertainmentCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(300);

    private static String URL = "https://3g.163.com/touch/reconstruct/article/list/BA10TA81wangning";

    private Map<String, NewsDO> cacheNewsDO = Maps.newHashMap();

    private Map<String, List<NewsImageDO>> cacheNewsImageDO = Maps.newHashMap();

    private List<String> URL_CACHE = Lists.newArrayList();

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private WangYiSportPipeline wangyiSportPipeline;

    private static int index = 0;

    @Override
    public Spider createCrawler() {

        ///0-20.html
        String url = String.format("%s/%s-20.html", URL, ++index);
        Request request = new Request(url);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);

        URL_CACHE.add(url);
        return spider;
    }

    @Override
    public synchronized void process(Page page) {
        if (URL_CACHE.contains(page.getUrl().toString())) {
            String jsonReturn = page.getRawText();

            // 重置 index 强制条件
            if (index > 200 && StringUtils.isEmpty(jsonReturn)) {
                index = 0;
            }

            // TODO: 2018/7/21 indexOut
            String jsonStr = jsonReturn.replaceAll("artiList", "");
            jsonStr = jsonStr.substring(jsonStr.indexOf("(") + 1, jsonStr.lastIndexOf(")"));
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            JSONArray dataJSONOArray = (JSONArray) jsonObject.get("BA10TA81wangning");
            for (int i = 0; i < dataJSONOArray.size(); i++) {
                //获取对象
                JSONObject jsonDataObject = (JSONObject) dataJSONOArray.get(i);
                //如果是直播页面 则直接丢弃
                Object skipURL = jsonDataObject.get("skipURL");
                if (!StringUtils.isEmpty(skipURL)) {
                    continue;
                }
                //来源 获取url
                Object newsSourceUrl = jsonDataObject.get("url");
                if (StringUtils.isEmpty(newsSourceUrl)) {
                    continue;
                }
                //标题
                Object title = jsonDataObject.get("title");
                //dataKey
                String dataKey = EncryptUtil.encrypt(String.valueOf(newsSourceUrl), "md5");
                //描述
                Object newsAbstract = jsonDataObject.get("digest");
                //新闻Id
                Long newsId = IDUtil.getNewID();
                //默认封面图有1张
                Integer imageCount = 1;
                List<NewsImageDO> newsImageDOList = new ArrayList<>();
                //封面图
                Object imgsrc = jsonDataObject.get("imgsrc");
                if (!StringUtils.isEmpty(imgsrc)) {

                    int width = 0;
                    int height = 0;
                    newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                            IDUtil.getNewID(), newsId,
                            imgsrc.toString(), width, height,
                            NewsImageDO.IMAGE_TYPE_MINI));
                } else {
                    continue;
                }
                //展示方式
                Integer displayType = handleNewsDisplayType(imageCount);
                //是否禁止评论 0允许
                Integer banComment = 0;
                //关键词
                String keywords = String.valueOf(jsonDataObject.get("source"))
                        .replaceAll("\\[", "")
                        .replaceAll("]", "")
                        .replaceAll("\"", "");
                Integer newsHot = 0;
                //发布时间
                Date displayTime = null;
                Object time = jsonDataObject.get("ptime");
                if (!StringUtils.isEmpty(time)) {
                    displayTime = DateUtil.parse(
                            String.valueOf(true), "yyyy-MM-dd HH:mm:ss");
                }
                String newsUrl = "";
                String shareUrl = "";
                Integer videoCount = 0;
                Integer sourceCommentCount = 0;
                //源评论数
                Object commentCount = jsonDataObject.get("commentCount");
                if (!StringUtils.isEmpty(commentCount)) {
                    sourceCommentCount = Integer.valueOf(commentCount.toString());
                }
                //信息来源
                Object source = jsonDataObject.get("source");
                NewsDO newsDO = newsCoreManager.buildNewsDO(
                        newsId, dataKey, title.toString(), newsHot,
                        newsUrl, shareUrl, source.toString(), newsSourceUrl.toString(),
                        NewsDO.NEWS_TYPE_ENTERTAINMENT,
                        NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                        keywords, banComment,
                        newsAbstract.toString(), displayTime,
                        videoCount, imageCount, null,
                        displayType, sourceCommentCount, 0);
                cacheNewsDO.put(newsDO.getDataKey(), newsDO);
                cacheNewsImageDO.put(newsDO.getDataKey(), newsImageDOList);
                //继续抓取详情页面
                Request request = new Request(newsSourceUrl.toString());
                page.addTargetRequest(request);
            }
        } else {
            String url = String.valueOf(page.getUrl());
            String dataKey = EncryptUtil.encrypt(url, "md5");
            Html html = page.getHtml();

            // 检查视频内容，有则过滤
            List<Selectable> nodes = html.xpath("//body").nodes();
            Selectable articleNode = nodes.get(0);
            //如果存在视频则整个都不需要
            List<Selectable> voideNodes = articleNode.xpath("//video").nodes();
            for (Selectable video : voideNodes) {
                Element elements = HtmlNodeUtil.getElements((HtmlNode) video).get(0);
                elements.remove();
                return;
            }

            Selectable contentNode = html.xpath("//div[@class='content']");

            ///
            /// 处理 img cdn 路径

            List<Selectable> imageNodes = contentNode.xpath("//img").nodes();

            String sourceAttr = "data-src";
            String targetAttr = "src";
            HtmlNodeUtil.handleCdnWithRemoveSourceAttr(imageNodes, sourceAttr, targetAttr);

            ///
            /// 获取正文内容，开始保存

            String content = contentNode.toString();
            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }

            // 去保存
            wangyiSportPipeline.saveNews(dataKey, cacheNewsDO, cacheNewsImageDO, content);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
