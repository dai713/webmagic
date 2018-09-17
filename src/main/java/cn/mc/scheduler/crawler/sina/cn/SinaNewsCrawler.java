package cn.mc.scheduler.crawler.sina.cn;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.util.HtmlNodeUtil;
import cn.mc.scheduler.util.SchedulerUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.nodes.Element;
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
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Selectable;
import java.util.*;

@Component
public class SinaNewsCrawler extends BaseCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SinaNewsCrawler.class);

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

//    private static final String URL = "https://cre.dp.sina.cn/api/v3/get?cateid=1z&cre=tianyi&mod=wtech&merge=3&statics=1&action=0&up=0&down=0&length=20";
    private static  String URL ="http://cre.dp.sina.cn/api/v3/get?callback=jQuery21405203954554129819_";

    private static Map<String, NewsDO> cacheNewsDO = Collections.EMPTY_MAP;

    private static Map<String, List<NewsImageDO>> cacheNewsImageDO = Collections.EMPTY_MAP;


    private  static Integer width = 240;

    private  static Integer high = 180;

    @Autowired
    private NewsCoreManager newsCoreManager;

    @Autowired
    private NewsImageCoreManager newsImageCoreManager;

    @Autowired
    private SinaNewsCrawlerPipeline sinaNewsCrawlerPipeline;

    private Long timeMillis;

    private  String initUrl;
    @Override
    public synchronized Spider createCrawler() {
        cacheNewsDO = new LinkedHashMap<>();
        cacheNewsImageDO = new LinkedHashMap<>();
        timeMillis=System.currentTimeMillis()/1000;
        StringBuffer urlStr=new StringBuffer(URL);
        urlStr.append(timeMillis).append("&offset=9&length=15&dedup=530&cre=techtop&mod=f&app_type=113&cateid=1z&statics=1&merge=3&_=").append(timeMillis);
        LOGGER.error("开始抓取新浪新闻:"+urlStr.toString());
        initUrl=urlStr.toString();
        Request request = new Request(initUrl);
        request.addHeader(HEADER_USER_AGENT_KEY, USER_AGENT_IPHONE_OS);
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        request.addHeader("Accept-Encoding", "gzip, deflate");
        request.addHeader("Accept-Language", "zh-CN,zh;q=0.9");
        request.addHeader("Cache-Control", "max-age=0");
        request.addHeader("Connection", "keep-alive");
        request.addHeader("Cookie", "ustat=__183.14.30.26_1528782335_0.36620000; genTime=1528782335; SINAGLOBAL=302922048938.2145.1528782336213; Apache=2003411070576.9749.1528783844705; ULV=1528783844706:2:2:2:2003411070576.9749.1528783844705:1528782336215; reco=usrmdinst_2; SMART=2; _SMART=1; dfz_loc=gd-shenzhen; SINAVID=4452b3762e981000; statuid=__183.14.30.26_1528789485_0.35069500; statuidsrc=Mozilla%2F5.0+%28Linux%3B+Android+6.0%3B+Nexus+5+Build%2FMRA58N%29+AppleWebKit%2F537.36+%28KHTML%2C+like+Gecko%29+Chrome%2F65.0.3325.181+Mobile+Safari%2F537.36%60183.14.30.26%60http%3A%2F%2Ft.cj.sina.cn%2Farticles%2Fview%2F5086790526%2Fm12f32437e045008col%3Fcre%3Dtianyi%26mod%3Dwtech%26loc%3D4%26r%3D0%26doct%3D0%26rfunc%3D47%26tj%3Dnone%26tr%3D12%26%26vt%3D4%26pos%3D18%60https%3A%2F%2Ftech.sina.cn%2F%3Fvt%3D4%26pos%3D18%60__183.14.30.26_1528789485_0.35069500; vt=4; historyRecord={\"href\":\"http://t.cj.sina.cn/\",\"refer\":\"http://t.cj.sina.cn/person_menu/index/\"}");
        request.addHeader("Host", "cre.dp.sina.cn");
        request.addHeader("Upgrade-Insecure-Requests", "1");

        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public void process(Page page) {
        if (initUrl.contains(page.getUrl().toString())) {
            String jsonReturn=page.getRawText();
            LOGGER.error("新浪新闻接口返回数据:"+jsonReturn);
            if(!StringUtils.isEmpty(jsonReturn)){
                String jsonStr=jsonReturn.replaceAll("jQuery21405203954554129819_"+timeMillis,"");
                jsonStr=jsonStr.substring(jsonStr.indexOf("(")+1,jsonStr.lastIndexOf(")"));
                JSONObject jsonObject = JSON.parseObject(jsonStr);
                JSONArray dataJSONOArray = (JSONArray) jsonObject.get("data");

                for (int i=0;i<dataJSONOArray.size();i++) {
                    //获取对象
                    JSONObject jsonDataObject = (JSONObject) dataJSONOArray.get(i);

                    //视频Id
                    Object videoId=jsonDataObject.get("video_id");
                    //如果存在视频的话过滤掉
                    if(!StringUtils.isEmpty(videoId)){
                        continue;
                    }
                    //来源 首先获取来源，确定手机显示的页面来源
                    Object newsSourceUrl = jsonDataObject.get("surl");
                    //如果没有手机适配则丢弃
                    if(StringUtils.isEmpty(newsSourceUrl)){
                        continue;
                    }

                    //标题
                    Object title = jsonDataObject.get("title");
                    if(StringUtils.isEmpty(title)){
                        title= jsonDataObject.get("ltitle");
                        //如果标题还是空则丢弃
                        if(StringUtils.isEmpty(title)){
                            continue;
                        }
                    }

                    //如果没有uuid则丢弃
                    String dataKey = EncryptUtil.encrypt(String.valueOf(title), "md5");
                    if(StringUtils.isEmpty(dataKey)){
                        continue;
                    }
                    //描述
                    Object newsAbstract = jsonDataObject.get("intro");
                    if(StringUtils.isEmpty(newsAbstract)){
                        newsAbstract= jsonDataObject.get("short_intro");
                    }
                    //新闻Id
                    Long newsId = IDUtil.getNewID();
                    //默认封面图有1张
                    Integer imageCount =1;
                    List<NewsImageDO> newsImageDOList = new ArrayList<>();
                    //封面图
                    JSONArray jsonArray = (JSONArray) jsonDataObject.get("mthumbs");
                    if(CollectionUtils.isEmpty(jsonArray)){
                        jsonArray = (JSONArray) jsonDataObject.get("thumbs");
                    }
                    if(!CollectionUtils.isEmpty(jsonArray)) {
                        imageCount = jsonArray.size();
                        for (Object imageObject : jsonArray) {
                            String imageUrl=imageObject.toString();
                            newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                                    IDUtil.getNewID(), newsId,
                                    imageUrl, width, high,
                                    NewsImageDO.IMAGE_TYPE_MINI));
                        }
                    }else{ //则是这取单张图片
                        Object imageUrl=jsonDataObject.get("thumb");
                        //如果还是没有则丢弃
                        if(StringUtils.isEmpty(imageUrl)){
                            continue;
                        }
                        newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                                IDUtil.getNewID(), newsId,
                                imageUrl.toString(), width, high,
                                NewsImageDO.IMAGE_TYPE_MINI));
                    }
                    //展示方式
                    Integer displayType = handleNewsDisplayType(imageCount);
                    //是否禁止评论 0允许
                    Integer banComment=0;
                    //关键词
                    //关键词
                    Object keywords = jsonDataObject.get("keywords");
                    if (StringUtils.isEmpty(keywords)) {
                        keywords=jsonDataObject.get("labels_show");
                        if(StringUtils.isEmpty(keywords)){
                            keywords = title;
                        }
                    }
                    keywords = keywords.toString().replaceAll("\\[", "")
                            .replaceAll("]", "")
                            .replaceAll("\"", "");
                    Integer newsHot = 0;
                    //发布时间
                    Date displayTime = null;
                    //创建时间
                    Date createTime=null;
                    //展示时间
                    Object time= jsonDataObject.get("mtime");
                    Object time1=jsonDataObject.get("ctime");
                    if(!StringUtils.isEmpty(time)){
                        try {
                            displayTime=new Date(Long.parseLong(time.toString())*1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if(!StringUtils.isEmpty(time1)){
                        try {
                            createTime=new Date(Long.parseLong(time1.toString())*1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    String newsUrl = "";
                    String shareUrl = "";
                    Integer videoCount = 0;
                    Integer sourceCommentCount =0;
                    //源评论数
                    Object commentCount=jsonDataObject.get("comment_count");
                    if(!StringUtils.isEmpty(commentCount)) {
                        sourceCommentCount=Integer.valueOf(commentCount.toString());
                    }
                    //信息来源
                    Object source = jsonDataObject.get("author");
                    if(StringUtils.isEmpty(source)){
                        source=jsonDataObject.get("media");
                    }
                    NewsDO newsDO = newsCoreManager.buildNewsDO(
                            newsId, dataKey, title.toString(), newsHot,
                            newsUrl, shareUrl, source.toString(), newsSourceUrl.toString(),
                            NewsDO.NEWS_TYPE_TECHNOLOGY,
                            NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                            keywords.toString(), banComment,
                            newsAbstract.toString(), displayTime,
                            videoCount, imageCount, createTime,
                            displayType, sourceCommentCount, 0);
                    cacheNewsDO.put(newsDO.getDataKey(), newsDO);
                    cacheNewsImageDO.put(newsDO.getDataKey(), newsImageDOList);
                    //继续抓取详情页面
                    Request request = new Request(newsSourceUrl.toString() + "&dataKey=" + dataKey);
                    page.addTargetRequest(request);
                }
            }
        }else {

            String url = String.valueOf(page.getUrl());
            Map<String, String> params = analyticalUrlParams(url);
            if (CollectionUtils.isEmpty(params)) {
                return;
            }

            if (!params.containsKey("dataKey")) {
                return;
            }
            String dataKey = params.get("dataKey");
            Html html = page.getHtml();
            String content="";
            List<Selectable> nodes;

            if(url.contains("tech.sina")){
                nodes= html.xpath("//article[@class='art_box']").nodes();
            }else if(url.contains("photo.sina.cn")){
                nodes= html.xpath("//article[@class='s_card']").nodes();
            }else{
                nodes = html.xpath("//div[@class='article']").nodes();
            }
            LOGGER.error("新浪新闻页面元数据:"+nodes.size());
            if(nodes.size()<=0){
                return;

            }
            Selectable articleNode = nodes.get(0);
            List<Selectable> imgNodes = articleNode.xpath("//img").nodes();
            for (Selectable imgNode : imgNodes) {
                Element elements = HtmlNodeUtil.getElements((HtmlNode) imgNode).get(0);
                if (imgNode.toString().contains("hide")) {
                    elements.remove();
                    continue;
                }
                if (imgNode.toString().contains("display:none")) {
                    elements.remove();
                    continue;
                }
                if (imgNode.toString().contains("ET8F-hcscwxc2926796.jpg")) {
                    elements.remove();
                    continue;
                }
                String dataSrc = elements.attr("data-src");
                if (!StringUtils.isEmpty(dataSrc)) {
                    elements.attr("src", dataSrc);
                }
            }
            List<Selectable> h1Nodes = articleNode.xpath("//h1").nodes();
            for (Selectable h1 : h1Nodes) {
                Element elements = HtmlNodeUtil.getElements((HtmlNode) h1).get(0);
                elements.remove();
            }
            List<Selectable> timeNodes = articleNode.xpath("//time").nodes();
            for (Selectable time : timeNodes) {
                Element elements = HtmlNodeUtil.getElements((HtmlNode) time).get(0);
                elements.remove();
            }
            List<Selectable> figureNodes = articleNode.xpath("//figure").nodes();
            for (Selectable figure : figureNodes) {
                Element elements = HtmlNodeUtil.getElements((HtmlNode) figure).get(0);
                if (figure.toString().contains("weibo_info")) {
                    elements.remove();
                }
            }
            List<Selectable> divNodes = articleNode.xpath("//div").nodes();
            for (Selectable div : divNodes) {
                Element elements = HtmlNodeUtil.getElements((HtmlNode) div).get(0);
                if (div.toString().contains("wx_pic")) {
                    elements.remove();
                }
            }
            List<Selectable> sectionNodes = articleNode.xpath("//section").nodes();
            for (Selectable section : sectionNodes) {
                Element elements = HtmlNodeUtil.getElements((HtmlNode) section).get(0);
                if (section.toString().contains("hide")) {
                    elements.remove();
                }
            }
            List<Selectable> scriptNodes = articleNode.xpath("//script").nodes();
            for (Selectable script : scriptNodes) {
                Element elements = HtmlNodeUtil.getElements((HtmlNode) script).get(0);
                elements.remove();
            }
            content = articleNode.toString();
            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }
            LOGGER.error("新浪新闻页面元数据保存:"+dataKey);
            // 去保存
            sinaNewsCrawlerPipeline.saveSinaNews(dataKey, cacheNewsDO, cacheNewsImageDO, content);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
