package cn.mc.scheduler.crawler.wa5.com;

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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.text.ParseException;
import java.util.*;
/**
 * 五星体育新闻
 *
 * @author xl
 * @date 2018/7/23 下午 15:46
 */
@Component
public class Wa5NewsCrawler extends BaseCrawler {

    private static final String URL = "https://wxsports_h5.dftoutiao.com/wxsports_h5/newspool?type=tuijian&typecode=20000223&startkey=&newkey=&pgnum=1&qid=null&domain=wxsports_h5&_=";

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private Map<String, NewsDO> cacheNewsDO = Maps.newHashMap();

    private Map<String, List<NewsImageDO>> cacheNewsImageDO = Maps.newHashMap();

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;

    private Long timeMillis;
    @Autowired
    private  Wa5NewsCrawlerPipeline wa5NewsCrawlerPipeline;

    @Override
    public Spider createCrawler() {
        timeMillis=System.currentTimeMillis();
        StringBuffer urlStr=new StringBuffer(URL);
        urlStr.append(timeMillis).append("&callback=Zepto").append(timeMillis);
        cacheNewsDO = new LinkedHashMap<>();
        cacheNewsImageDO = new LinkedHashMap<>();
        Request request = new Request(URL);
        request.addHeader(HEADER_USER_AGENT_KEY, USER_AGENT_IPHONE_OS);
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
                JSONArray dataJSONOArray = (JSONArray) jsonObject.get("data");
                for (int i = 0; i < dataJSONOArray.size(); i++) {
                    //获取对象
                    JSONObject jsonDataObject = (JSONObject) dataJSONOArray.get(i);
                    //标题
                    Object title = jsonDataObject.get("topic");
                    if (StringUtils.isEmpty(title)) {
                            continue;
                    }
                    Object videoLink = jsonDataObject.get("video_link");
                    if (!StringUtils.isEmpty(videoLink)) {
                        continue;
                    }
                    //来源 首先获取来源，确定手机显示的页面来源
                    Object newsSourceUrl = jsonDataObject.get("url");
                    //如果没有手机适配则丢弃
                    if (StringUtils.isEmpty(newsSourceUrl)) {
                        continue;
                    }
//                    String dataKey = EncryptUtil.encrypt(String.valueOf(title), "md5");
                    String dataKey=EncryptUtil.encrypt(newsSourceUrl.toString(), "md5");
                    //描述
                    Object newsAbstract = jsonDataObject.get("topic");
                    //新闻Id
                    Long newsId = IDUtil.getNewID();
                    List<NewsImageDO> newsImageDOList = new ArrayList<>();
                    //封面图
                    JSONArray jsonArray = (JSONArray)jsonDataObject.get("miniimg");
                    if (CollectionUtils.isEmpty(jsonArray)) {
                        jsonArray = (JSONArray) jsonDataObject.get("lbimg");
                    }
                    boolean checkImgUrl=true;
                    if (!CollectionUtils.isEmpty(jsonArray)) {

                        for(int j=0;j<jsonArray.size();j++){
                            Integer imgWidth=0;
                            Integer imgHigh=0;
                            JSONObject imgObject = (JSONObject) jsonArray.get(j);
                            //获取图片的宽
                            Object objectWidth=imgObject.get("imgwidth");
                            if(!StringUtils.isEmpty(objectWidth)){
                                imgWidth=Integer.parseInt(objectWidth.toString());
                            }
                            //获取图片的高
                            Object objectHigh=imgObject.get("imgheight");
                            if(!StringUtils.isEmpty(objectWidth)){
                                imgHigh=Integer.parseInt(objectHigh.toString());
                            }
                            String imageUrl=imgObject.get("src").toString();
                            if(StringUtils.isEmpty(imageUrl)){
                                checkImgUrl=false;
                            }
                            newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                                    IDUtil.getNewID(), newsId,
                                    imageUrl, imgWidth, imgHigh,
                                    NewsImageDO.IMAGE_TYPE_MINI));
                        }
                    }
                    //如果存在封面图获取不到的情况则丢弃
                    if(checkImgUrl==false){
                        continue;
                    }
                    //展示方式
                    Integer displayType = handleNewsDisplayType(newsImageDOList.size());
                    //是否禁止评论 0允许
                    Integer banComment = 0;
                    //关键词
                    Object keywords = jsonDataObject.get("showtags");
                    if(StringUtils.isEmpty(keywords)) {
                        keywords = title;
                    }
                    Integer newsHot = 0;
                    //发布时间
                    Date displayTime = null;
                    //创建时间
                    Date createTime = null;
                    Object time = jsonDataObject.get("date");
                    if (!StringUtils.isEmpty(time)) {
                        try {
                           displayTime= DateUtil.parse(time.toString(),DateUtil.DATE_FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND2);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        continue;
                    }
                    String newsUrl = "";
                    String shareUrl = "";
                    Integer videoCount = 0;
                    Integer sourceCommentCount = 0;
                    //信息来源
                    Object source = jsonDataObject.get("source");
                    NewsDO newsDO = newsCoreManager.buildNewsDO(
                            newsId, dataKey, title.toString(), newsHot,
                            newsUrl, shareUrl, source.toString(), newsSourceUrl.toString(),
                            NewsDO.NEWS_TYPE_SPORTS,
                            NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                            keywords.toString(), banComment,
                            newsAbstract.toString(), displayTime,
                            videoCount, newsImageDOList.size(), createTime,
                            displayType, sourceCommentCount, 0);
                    cacheNewsDO.put(newsDO.getDataKey(), newsDO);
                    cacheNewsImageDO.put(newsDO.getDataKey(), newsImageDOList);
                    //继续抓取详情页面
                    Request request = new Request(newsSourceUrl.toString());
                    request.setMethod("GET");
                    page.addTargetRequest(request);
                }
            }

        }else{
            String url = String.valueOf(page.getUrl());
            String dataKey = EncryptUtil.encrypt(url, "md5");
            Html html = page.getHtml();
            List<Selectable> nodes = html.xpath("//div[@class='J-article-content article-content']").nodes();
            if (CollectionUtils.isEmpty(nodes))
                return;
            Selectable articleNode = nodes.get(0);
            String content = articleNode.toString();
            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }
            wa5NewsCrawlerPipeline.saveWa5News(dataKey, cacheNewsDO, cacheNewsImageDO, content);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
