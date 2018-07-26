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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class WangYiMilitaryCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static String URL = "https://3g.163.com/touch/reconstruct/article/list/BAI67OGGwangning/0-20.html";

    private Map<String, NewsDO> cacheNewsDO = Maps.newHashMap();

    private Map<String, List<NewsImageDO>> cacheNewsImageDO = Maps.newHashMap();

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private WangYiSportPipeline wangyiSportPipeline;

    @Override
    public Spider createCrawler() {
        Request request = new Request(URL);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public synchronized void process(Page page) {
        if (URL.contains(page.getUrl().toString())) {
            String jsonReturn = page.getRawText();
            String jsonStr = jsonReturn.replaceAll("artiList", "");
            jsonStr = jsonStr.substring(jsonStr.indexOf("(") + 1, jsonStr.lastIndexOf(")"));
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            JSONArray dataJSONOArray = (JSONArray) jsonObject.get("BAI67OGGwangning");

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
                    displayTime = DateUtil.parse(String.valueOf(time),
                            "yyyy-MM-dd HH:mm:ss");
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
                        NewsDO.NEWS_TYPE_MILITARY,
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
            StringBuffer content = new StringBuffer();
            //视频对象获取
            if (url.contains("video")) {
                //video标签
                List<Selectable> videoDivNodes = html.xpath("//div[@class='video']").nodes();
                if (null != videoDivNodes && videoDivNodes.size() > 0) {
                    content.append(videoDivNodes.get(0).toString());
                }
                //内容
                List<Selectable> videoAreaNodes = html.xpath("//div[@class='video_area']").nodes();
                if (null != videoAreaNodes && videoAreaNodes.size() > 0) {
                    content.append(videoAreaNodes.get(0).toString());
                }
            } else {
                String contentStr = html.xpath("//div[@class='content']").toString();
                content.append(contentStr);
            }
            String saveComment = content.toString();
            if (StringUtils.isEmpty(saveComment)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }
            // 替换内容
            saveComment = saveComment.replaceAll("data-src", "src");
            // 去保存
            wangyiSportPipeline.saveNews(dataKey, cacheNewsDO, cacheNewsImageDO, saveComment);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
