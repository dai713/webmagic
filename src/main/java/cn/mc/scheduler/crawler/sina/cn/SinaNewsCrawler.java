package cn.mc.scheduler.crawler.sina.cn;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.dataObject.SystemKeywordsDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.util.SchedulerUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class SinaNewsCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

//    private static final String URL = "https://cre.dp.sina.cn/api/v3/get?cateid=1z&cre=tianyi&mod=wtech&merge=3&statics=1&action=0&up=0&down=0&length=20";
    private static  String URL ="http://cre.dp.sina.cn/api/v3/get?callback=jQuery21405203954554129819_";

    private static Map<String, NewsDO> cacheNewsDO = Collections.EMPTY_MAP;

    private static Map<String, List<NewsImageDO>> cacheNewsImageDO = Collections.EMPTY_MAP;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private  static Integer width = 240;

    private  static Integer high = 180;

    @Autowired
    private NewsCoreManager newsCoreManager;

    @Autowired
    private NewsImageCoreManager newsImageCoreManager;

    @Autowired
    private SinaNewsCrawlerPipeline sinaNewsCrawlerPipeline;

    @Autowired
    private SchedulerUtils schedulerUtils;

    private Long timeMillis;

    @Override
    public synchronized Spider createCrawler() {
        cacheNewsDO = new LinkedHashMap<>();
        cacheNewsImageDO = new LinkedHashMap<>();
        timeMillis=System.currentTimeMillis()/1000;
        StringBuffer urlStr=new StringBuffer(URL);
        urlStr.append(timeMillis).append("&offset=9&length=15&dedup=530&cre=techtop&mod=f&app_type=113&cateid=1z&statics=1&merge=3&_=").append(timeMillis);
        URL=urlStr.toString();
        Request request = new Request(URL);
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
        if (URL.contains(page.getUrl().toString())) {
            String jsonReturn=page.getRawText();
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
                            displayTime=sdf.parse(sdf.format(new Date(Long.parseLong(time.toString())*1000)));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    if(!StringUtils.isEmpty(time1)){
                        try {
                            createTime=sdf.parse(sdf.format(new Date(Long.parseLong(time1.toString())*1000)));
                        } catch (ParseException e) {
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
            String content = html.xpath("//div[@class='article']").toString();


            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }

            // 去保存
            sinaNewsCrawlerPipeline.saveSinaNews(dataKey, cacheNewsDO, cacheNewsImageDO, content);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
