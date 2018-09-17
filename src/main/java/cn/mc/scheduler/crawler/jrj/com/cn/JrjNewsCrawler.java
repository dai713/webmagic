package cn.mc.scheduler.crawler.jrj.com.cn;

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
 * 金融界财经资讯
 *
 * @author xl
 * @time 2018/8/09 15:51
 */
@Component
public class JrjNewsCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static  String URL ="http://m.jrj.com.cn/api/news/indexNews?n=50&date=&id=";

    private static Map<String, NewsDO> cacheNewsDO = Collections.EMPTY_MAP;

    private static Map<String, List<NewsImageDO>> cacheNewsImageDO = Collections.EMPTY_MAP;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private JrjCrawlerPipeline jrjCrawlerPipeline;

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
                JSONArray dataJSONOArray = (JSONArray) jsonObject.get("data");
                for (int i = 0; i < dataJSONOArray.size(); i++) {
                    //获取对象
                    JSONObject jsonDataObject = (JSONObject) dataJSONOArray.get(i);
                    //来源 首先获取来源，确定手机显示的页面来源
                    String newsSourceUrl = jsonDataObject.get("mInfoUrl").toString();
                    //如果没有手机适配则丢弃
                    if(StringUtils.isEmpty(newsSourceUrl)){
                        continue;
                    }
                    //标题
                    String title = jsonDataObject.get("title").toString();
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
                    Object newsAbstract = jsonDataObject.get("detail");
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
                    Object source = jsonDataObject.get("paperMediaSource");
                    Object keywords = jsonDataObject.get("keyword");
                    if(StringUtils.isEmpty(keywords)){
                        keywords=title;
                    }
                    //发布时间
                    Date displayTime =  DateUtil.parse(String.valueOf(
                            jsonDataObject.get("listDate")),
                            "yyyy-MM-dd HH:mm:ss");
                    String newsUrl = "";
                    String shareUrl = "";
                    Integer videoCount = 0;
                    Integer newsHot = 0;
                    Integer sourceCommentCount =0;
                    List<NewsImageDO> newsImageDOList = new ArrayList<>();
                    //封面图
                    JSONArray jsonArray = (JSONArray) jsonDataObject.get("imgUrl");
                    //如果存在封面图
                    if (!CollectionUtils.isEmpty(jsonArray)) {
                        for (int j=0;j<jsonArray.size();j++) {
                            //只取三张
                            if(j<3){
                                String imageUrl = jsonArray.get(j).toString();
                                if (StringUtils.isEmpty(imageUrl)) {
                                    continue;
                                }
                                newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                                        IDUtil.getNewID(), newsId,
                                        imageUrl, 0, 0,
                                        NewsImageDO.IMAGE_TYPE_MINI));
                            }
                        }
                    }
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
                    request=addHeader(request);
                    page.addTargetRequest(request);
                }
            }
        }else{
            String url = String.valueOf(page.getUrl());
            String dataKey = EncryptUtil.encrypt(url, "md5");
            Html html = page.getHtml();
            List<Selectable> nodesContent=html.xpath("//div[@class='wenzi']").nodes();
            String content=nodesContent.get(0).toString();

            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }
            content = content.replaceAll("<a", "<div");
            content = content.replaceAll("a>", "div>");
            jrjCrawlerPipeline.saveJrjNews(dataKey, cacheNewsDO, cacheNewsImageDO, content);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
    private Request addHeader(Request request) {
        request.addHeader(HEADER_USER_AGENT_KEY, USER_AGENT_IPHONE_OS);
        request.addHeader("Host", "m.jrj.com.cn");
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        request.addHeader("Upgrade-Insecure-Requests", "1");
        request.addHeader("Accept-Encoding", "gzip, deflate, br");
        request.addHeader("Accept-Language", "zh-CN,zh;q=0.9");
        request.addHeader("Cache-Control", "no-cache");
        request.addHeader("Connection", "keep-alive");
        request.addHeader("Pragma","no-cache");
        request.addHeader("Cookie","fcStop=yes; ADVC=367e667cfecd99; ADVS=367e667cfecd99; ASL=17753,0000i,b70e8726; channelCode=3763BEXX; ylbcode=24S2AZ96; Hm_lvt_0359dbaa540096117a1ec782fff9c43f=1533862739; vjuids=25566252.1652157fd21.0.45cc842790f4a; vjlast=1533862739.1533862739.30; login=failed; Hm_lvt_8e6b22533be2449b420f6a8f7abab6e4=1533862745; Hm_lpvt_0359dbaa540096117a1ec782fff9c43f=1533862780; jrj_uid=15338645531648B9QIPigBT; BIGipServerpool_MemcachedB_80=583274668.20480.0000; jrj_z3_newsid=31183; WT_FPC=id=2c37d5bb330a9c85cc51533862745108:lv=1533870003228:ss=1533869290533; Hm_lpvt_8e6b22533be2449b420f6a8f7abab6e4=1533870003");return request;
    }
}
