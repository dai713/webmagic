package cn.mc.scheduler.crawler.ithome.com;

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
import com.google.common.collect.Maps;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.model.HttpRequestBody;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Selectable;
import us.codecraft.webmagic.utils.HttpConstant;

import java.util.*;


@Component
public class ITHomeNewsCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static final String URL = "https://m.ithome.com/api/news/newslistpageget?Tag=news&ot=1528369807000&page=0";

    private Map<String, NewsDO> cacheNewsDO = Maps.newHashMap();

    private Map<String, List<NewsImageDO>> cacheNewsImageDO = Maps.newHashMap();

    private Map cacheTitle = new HashMap();

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private ITHomeNewsCrawlerPipeline itHomeNewsCrawlerPipeline;

    @Override
    public BaseSpider createCrawler() {
        Request request = new Request(URL);
        request.setMethod(HttpConstant.Method.POST);
        request.setRequestBody(HttpRequestBody.json("{}","utf-8"));

        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public synchronized void process(Page page) {

        if (URL.contains(page.getUrl().toString())) {

            if(!StringUtils.isEmpty(page.getRawText())){

                JSONObject jsonObject = JSON.parseObject(page.getRawText());
                JSONArray dataJSONOArray = (JSONArray) jsonObject.get("Result");

//                for (Object object : dataJSONOArray) {
                for (int i=0;i<dataJSONOArray.size();i++) {
                    //获取对象
                    JSONObject jsonDataObject = (JSONObject) dataJSONOArray.get(i);

                    //标题
                    String title = String.valueOf(jsonDataObject.get("title")).trim();

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
                    //广告id
                    Object lapinid=jsonDataObject.get("lapinid");
                    if(!StringUtils.isEmpty(lapinid)){
                         continue;
                    }
                    //新闻标签
                    Object newsTips=jsonDataObject.get("NewsTips");
                    if(!StringUtils.isEmpty(newsTips)){
                        //过滤广告
                        if(newsTips.toString().contains("广告")){
                            continue;
                        }
                    }
                    //来源
                    String newsSourceUrl = String.valueOf(jsonDataObject.get("WapNewsUrl"));

                    //描述
                    String newsAbstract = String.valueOf(jsonDataObject.get("description")).trim();
                    Long newsId = IDUtil.getNewID();
                    //封面图
                    JSONArray jsonArray = (JSONArray) jsonDataObject.get("imagelist");
                    //默认封面图有1张
                    Integer imageCount =1;
                    List<NewsImageDO> newsImageDOList = new ArrayList<>();
                    if(!CollectionUtils.isEmpty(jsonArray)){
                        imageCount=jsonArray.size();
                        boolean checkImg=true;
                        for (Object imageObject : jsonArray) {
                            if(null!=imageObject){
                                String imageUrl=imageObject.toString().substring(0,imageObject.toString().indexOf("@"));
                                //添加校验问题
                                if(StringUtils.isEmpty(imageUrl) && !imageUrl.startsWith("http:") && !imageUrl.startsWith("https:")){
                                    checkImg=false;
                                }
                                String imageSWH=imageObject.toString().substring(imageObject.toString().indexOf("@")+1,imageObject.toString().length());
                                String[] swh=imageSWH.split(",");
                                if(swh.length>0){
                                    String widthStr=swh[1];
                                    String highStr=swh[2];
                                    Integer width=Integer.parseInt(widthStr.substring(widthStr.indexOf("_")+1,widthStr.length()));
                                    Integer high=Integer.parseInt(highStr.substring(highStr.indexOf("_")+1,highStr.length()));
                                    newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                                            IDUtil.getNewID(), newsId,
                                            imageUrl, width, high,
                                            NewsImageDO.IMAGE_TYPE_MINI));
                                }
                            }
                        }
                        //如果图片有问题则丢弃当前新闻
                        if(checkImg==false){
                            continue;
                        }
                    }else{
                        //IT之家默认图片宽高
                        Integer width=240;
                        Integer high=180;
                        String imageUrl= String.valueOf(jsonDataObject.get("image"));
                        //如果还是没封面图 丢弃当前
                        if(StringUtils.isEmpty(imageUrl)){
                            continue;
                        }
                        newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                                IDUtil.getNewID(), newsId,
                                imageUrl, width, high,
                                NewsImageDO.IMAGE_TYPE_MINI));
                    }
                    Integer displayType = handleNewsDisplayType(imageCount);
//                    String newsKey = String.valueOf(jsonDataObject.get("newsid"));
                    String dataKey=newsSourceUrl;
                    dataKey=EncryptUtil.encrypt(dataKey, "md5");

                    //是否禁止评论 0允许
                    Integer banComment=0;
                    Integer newsHot = 0;
                    String keywords = String.valueOf(jsonDataObject.get("title"));
                    Integer videoCount = 0;
                    //展示时间
                    String time=jsonDataObject.get("orderdate").toString().replaceAll("T"," ");
                    String time1=jsonDataObject.get("postdate").toString().replaceAll("T"," ");
                    //发布时间
                    Date displayTime = null;
                    //创建时间
                    Date createTime=null;
                    try {
                        displayTime= DateUtil.parse(time,DateUtil.DATE_FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND2);;
                        createTime=DateUtil.parse(time1,DateUtil.DATE_FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND2);;
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }

                    String newsUrl = "";
                    String shareUrl = "";
                    //源评论数
                    Integer sourceCommentCount = Integer.valueOf(jsonDataObject.get("commentcount").toString());
                    NewsDO newsDO = newsCoreManager.buildNewsDO(
                            newsId, dataKey, title, newsHot,
                            newsUrl, shareUrl, "", newsSourceUrl,
                            NewsDO.NEWS_TYPE_TECHNOLOGY,
                            NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                            keywords, banComment,
                            newsAbstract, displayTime,
                            videoCount, imageCount, createTime,
                            displayType, sourceCommentCount, 0);
                    cacheNewsDO.put(newsDO.getDataKey(), newsDO);
                    cacheNewsImageDO.put(newsDO.getDataKey(), newsImageDOList);
                    //继续抓取详情页面
                    Request request = new Request(newsSourceUrl);
                    page.addTargetRequest(request);
                }
            }
        }else{
            String url= String.valueOf(page.getUrl());
//            String newsKey=url.substring(url.lastIndexOf("/")+1,url.lastIndexOf("."));
            Html html = page.getHtml();
            List<Selectable> nodes = html.xpath("//div[@class='con-box']").nodes();

            for (Selectable node : nodes) {
                //生成dataKey
                String dataKey=url;
                dataKey=EncryptUtil.encrypt(dataKey, "md5");
                //获取内容
                Selectable contentNode = node.xpath("//div[@class='news-content']");
                List<Selectable> aNodes = contentNode.xpath("//a").nodes();
                for (Selectable aNode : aNodes) {
                    if (aNode.toString().contains("href")) {
                        Element elements = HtmlNodeUtil.getElements((HtmlNode) aNode).get(0);
                        elements.remove();
                        continue;
                    }
                }

                String content = contentNode.toString();
                if (StringUtils.isEmpty(content)
                        || StringUtils.isEmpty(dataKey)
                        || !cacheNewsDO.containsKey(dataKey)) {
                    return;
                }

                // 获取来源
                String source = node.xpath("//span[@class='news-author']/tidyText()").toString();
                //将内容的data-original的改为src
                content = content.replaceAll("data-original","src");
                // 去保存
                itHomeNewsCrawlerPipeline.saveITHomeNews(dataKey, cacheNewsDO, cacheNewsImageDO, content,source);
            }

        }

    }
    public static Integer handleNewsDisplayType(Integer imageSize) {
        if (imageSize >= 3) {
            return NewsDO.DISPLAY_TYPE_THREE_MINI_IMAGE;
        } else if (imageSize >= 1) {
            return NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE;
        } else {
            return NewsDO.DISPLAY_TYPE_LINE_TEXT;
        }
    }
    @Override
    public Site getSite() {
        return site;
    }
}
