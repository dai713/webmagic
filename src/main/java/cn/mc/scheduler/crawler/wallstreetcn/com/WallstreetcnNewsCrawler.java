package cn.mc.scheduler.crawler.wallstreetcn.com;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.crawler.jrj.com.cn.JrjCrawlerPipeline;
import cn.mc.scheduler.util.HtmlNodeUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Selectable;

import java.util.*;

/**
 * 华尔街见闻（财经） 资讯
 *
 * @author xl
 * @time 2018/8/09 15:51
 */
@Component
public class WallstreetcnNewsCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static  String URL ="https://m.wallstreetcn.com/apiv1/content/fabricate-articles?limit=20&cursor=&channel=global&accept=article,newsroom,morning-report,newsrooms,live,calendar,audition,wits-home-users,hot-themes,ad.internal_banner.inhouse,ad.internal_inline.inhouse,ad.inline.inhouse,ad.video.inhouse,ad.banner.inhouse,ad.inline.plista,ad.banner.plista,ad.topic.inhouse,ad.inline.tanx";

    private static Map<String, NewsDO> cacheNewsDO = Collections.EMPTY_MAP;

    private static Map<String, List<NewsImageDO>> cacheNewsImageDO = Collections.EMPTY_MAP;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private WallstreetcnCrawlerPipeline wallstreetcnCrawlerPipeline;

    private Map cacheTitle = new HashMap();
    @Override
    public Spider createCrawler() {
        cacheNewsDO = new LinkedHashMap<>();
        cacheNewsImageDO = new LinkedHashMap<>();
        Request request = new Request(URL);
        request=addHeader(request);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public void process(Page page) {

        if (URL.contains(page.getUrl().toString())) {
            String jsonReturn = page.getRawText();
            if (!StringUtils.isEmpty(jsonReturn)) {
                JSONObject jsonObject = JSON.parseObject(jsonReturn);
                JSONObject jsonData = (JSONObject) jsonObject.get("data");
                JSONArray dataJSONOArray = (JSONArray)jsonData.get("items");
                for (int i = 0; i < dataJSONOArray.size(); i++) {
                    //获取对象
                    JSONObject jsonDataObject = (JSONObject) dataJSONOArray.get(i);
                    String type = jsonDataObject.get("resource_type").toString();
                    JSONObject jsonDataObject1=JSON.parseObject(jsonDataObject.get("resource").toString());
                    //如果不是图文 则丢弃
                    if(!type.equals("article")){
                        continue;
                    }

                    //来源 首先获取来源，确定手机显示的页面来源
                    String newsSourceUrl = jsonDataObject1.get("uri").toString();
                    //如果没有手机适配则丢弃
                    if(StringUtils.isEmpty(newsSourceUrl)){
                        continue;
                    }
                    //标题
                    String title = jsonDataObject1.get("title").toString();
                    if(StringUtils.isEmpty(title)){
                        continue;
                    }
                    String cTitle= (String) cacheTitle.get("title");

                    //如果是第1行，是此批次的最新时间
                    if(i==0){
                        cacheTitle.put("title",title);
                    }
                    //因为是按时间排序的只要处理到有相同的标题则结束抓取
                    if(!StringUtils.isEmpty(cTitle)){
                        if(cTitle.equals(title)){
                            break;
                        }
                    }
                    //如果没有uuid则丢弃
                    String dataKey = EncryptUtil.encrypt(newsSourceUrl, "md5");
                    //描述
                    Object newsAbstract = jsonDataObject1.get("content_short");
                    if(StringUtils.isEmpty(newsAbstract)){
                        newsAbstract=title;
                    }
                    if (newsAbstract.toString().length() > 100) {
                        newsAbstract = newsAbstract.toString().substring(0, 100);
                    }
                    //新闻Id
                    Long newsId = IDUtil.getNewID();
                    //是否禁止评论 0允许
                    Integer banComment=0;
                    //信息来源
                    String source = "华尔街见闻";
                    JSONObject author = (JSONObject) jsonDataObject1.get("author");
                    Object authors=author.get("display_name");
                    if(!StringUtils.isEmpty(authors)){
                        source=authors.toString();
                    }
                    StringBuffer keywords = new StringBuffer();
                    JSONArray keywordJSONOArray = (JSONArray)jsonDataObject1.get("related_themes");
                    if(null!=keywordJSONOArray){
                        for(int j = 0; j < keywordJSONOArray.size(); j++){
                            JSONObject keywordObject = (JSONObject) keywordJSONOArray.get(j);
                            keywords.append(keywordObject.get("title")).append(",");
                        }
                        if(StringUtils.isEmpty(keywords)){
                            keywords.append(title);
                        }
                    }
                    //发布时间
                    Date displayTime = null;
                    try {
                        displayTime=new Date(Long.parseLong(jsonDataObject1.get("display_time").toString()));
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                    String newsUrl = "";
                    String shareUrl = "";
                    Integer videoCount = 0;
                    Integer newsHot = 0;
                    Integer sourceCommentCount =0;
                    List<NewsImageDO> newsImageDOList = new ArrayList<>();
                    //封面图
                    String imageUrl = jsonDataObject1.get("image_uri").toString();
                    if (StringUtils.isEmpty(imageUrl)) {
                        continue;
                    }
                    newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                            IDUtil.getNewID(), newsId,
                            imageUrl, 0, 0,
                            NewsImageDO.IMAGE_TYPE_MINI));
                    //如果存在封面图
                    Integer imageCount=newsImageDOList.size();
                    //展示方式
                    Integer displayType = handleNewsDisplayType(imageCount);
                    NewsDO newsDO = newsCoreManager.buildNewsDO(
                            newsId, dataKey, title, newsHot,
                            newsUrl, shareUrl, source.toString(), newsSourceUrl,
                            NewsDO.NEWS_TYPE_FINANCE,
                            NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                            keywords.toString(), banComment,
                            newsAbstract.toString(), displayTime,
                            videoCount, imageCount, null,
                            displayType, sourceCommentCount, 0);
                    cacheNewsDO.put(newsDO.getDataKey(), newsDO);
                    //继续抓取详情页面
                    Request request = new Request(newsSourceUrl.toString());
                    page.addTargetRequest(request);
                }
            }
        }else{
            String url = String.valueOf(page.getUrl());
            String dataKey = EncryptUtil.encrypt(url, "md5");
            Html html = page.getHtml();
            List<Selectable> nodesContent=html.xpath("//div[@class='node-article-content']").nodes();
            if(nodesContent.size()<=0){
                return;
            }
            Selectable articleNode = nodesContent.get(0);
            List<Selectable> pNodes = articleNode.xpath("//p").nodes();
            for (Selectable p : pNodes) {
                Element elements = HtmlNodeUtil.getElements((HtmlNode) p).get(0);
                if (p.toString().contains("华尔街见闻APP")) {
                    elements.remove();
                }
            }
            String content=articleNode.toString();
            content = content.replaceAll("<a", "<div");
            content = content.replaceAll("a>", "div>");
            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }
            wallstreetcnCrawlerPipeline.saveWallstreetcnNews(dataKey, cacheNewsDO, cacheNewsImageDO, content);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
    private Request addHeader(Request request) {
        request.addHeader(HEADER_USER_AGENT_KEY, USER_AGENT_IPHONE_OS);
        request.addHeader(":authority", "m.wallstreetcn.com");
        request.addHeader(":path", "/apiv1/content/fabricate-articles?limit=20&cursor=&channel=global&accept=article,newsroom,morning-report,newsrooms,live,calendar,audition,wits-home-users,hot-themes,ad.internal_banner.inhouse,ad.internal_inline.inhouse,ad.inline.inhouse,ad.video.inhouse,ad.banner.inhouse,ad.inline.plista,ad.banner.plista,ad.topic.inhouse,ad.inline.tanx");
        request.addHeader(":scheme", "https");
        request.addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        request.addHeader("accept-encoding", "gzip, deflate, br");
        request.addHeader("accept-language", "zh-CN,zh;q=0.9");
        request.addHeader("cache-control", "no-cache");
        request.addHeader("pragma","no-cache");
        request.addHeader("upgrade-insecure-requests","1");
        request.addHeader("cookie","pcwscn-device-id=pcwscn-16522f25-9012-8840-03fd-8cc4566258f5; _ga=GA1.2.864135750.1533889633; _gid=GA1.2.1102431115.1533889633; mwscn-device-id=mwscn-16522f28-5556-233a-a196-663b4a6490d7");
        return request;
    }
}
