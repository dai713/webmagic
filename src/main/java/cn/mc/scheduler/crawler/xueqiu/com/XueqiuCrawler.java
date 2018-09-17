package cn.mc.scheduler.crawler.xueqiu.com;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.util.*;
/**
 * 雪球资讯
 *
 * @author xl
 * @time 2018/8/09 15:51
 */
@Component
public class XueqiuCrawler  extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static  String URL ="https://xueqiu.com/v4/statuses/public_timeline_by_category.json?since_id=-1&max_id=-1&count=10&category=-1";

    private static String href="https://xueqiu.com";

    private static Map<String, NewsDO> cacheNewsDO = Collections.EMPTY_MAP;

    private static Map<String, List<NewsImageDO>> cacheNewsImageDO = Collections.EMPTY_MAP;

    @Autowired
    private NewsCoreManager newsCoreManager;

    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private XueqiuCrawlerPipeline xueqiuCrawlerPipeline;

    @Override
    public Spider createCrawler() {
        cacheNewsDO = new LinkedHashMap<>();
        cacheNewsImageDO = new LinkedHashMap<>();
        Request request = new Request(URL);
        request.addHeader(HEADER_USER_AGENT_KEY, USER_AGENT_IPHONE_OS);
        request.addHeader("Host", "xueqiu.com");
        request.addHeader("Upgrade-Insecure-Requests", "1");
        request.addHeader("Accept-Encoding", "gzip, deflate, br");
        request.addHeader("Accept-Language", "zh-CN,zh;q=0.9");
        request.addHeader("Cache-Control", "no-cache");
        request.addHeader("Connection", "keep-alive");
        request.addHeader("Pragma","no-cache");
        request.addHeader("Cookie","_ga=GA1.2.255561264.1530783825; device_id=e1db4cd609f01a04027c80f0d62d43c5; s=ej125nsm3f; _gid=GA1.2.1145322492.1533798072; __utma=1.255561264.1530783825.1533798111.1533798111.1; __utmz=1.1533798111.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); aliyungf_tc=AQAAAGlpyH3enAIAEIcOt4D3IwyHJqov; xq_a_token=aef774c17d4993658170397fcd0faedde488bd20; xq_a_token.sig=F7BSXzJfXY0HFj9lqXif9IuyZhw; xq_r_token=d694856665e58d9a55450ab404f5a0144c4c978e; xq_r_token.sig=Ozg4Sbvgl2PbngzIgexouOmvqt0; u=291533799011053; Hm_lvt_1db88642e346389874251b5a1eded6e3=1533798072,1533799011; Hm_lpvt_1db88642e346389874251b5a1eded6e3=1533814463");
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
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
                JSONArray dataJSONOArray = (JSONArray) jsonObject.get("list");
                for (int i = 0; i < dataJSONOArray.size(); i++) {
                    //获取对象
                    JSONObject jsonDataObject = (JSONObject) dataJSONOArray.get(i);
                    JSONObject jsonDataObject1=JSON.parseObject(jsonDataObject.get("data").toString());
                    //来源 首先获取来源，确定手机显示的页面来源
                    Object newsSourceUrl = jsonDataObject1.get("target");
                    newsSourceUrl=href+newsSourceUrl;
                    //如果没有手机适配则丢弃
                    if(StringUtils.isEmpty(newsSourceUrl)){
                        continue;
                    }
                    //标题
                    Object title = jsonDataObject1.get("title");
                    if(StringUtils.isEmpty(title)){
                        continue;
                    }
                    //如果没有uuid则丢弃
                    String dataKey = EncryptUtil.encrypt(String.valueOf(newsSourceUrl), "md5");
                    //描述
                    Object newsAbstract = jsonDataObject1.get("description");
                    if(StringUtils.isEmpty(newsAbstract)){
                        newsAbstract=title;
                    }
                    if (newsAbstract.toString().length() > 50) {
                        newsAbstract = newsAbstract.toString().substring(0, 50);
                    }
                    //新闻Id
                    Long newsId = IDUtil.getNewID();
                    //是否禁止评论 0允许
                    Integer banComment=0;
                    //信息来源
                    Object source = jsonDataObject1.get("source");
                    Object keywords = jsonDataObject1.get("screen_name");
                    if(StringUtils.isEmpty(keywords)){
                        keywords=title;
                    }
                    Object time=jsonDataObject1.get("created_at");
                    //发布时间
                    Date displayTime = null;
                    try {
                        displayTime=new Date(Long.parseLong(time.toString())/1000);
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                    String newsUrl = "";
                    String shareUrl = "";
                    Integer videoCount = 0;
                    Integer newsHot = 0;

                    Integer sourceCommentCount =0;
                    //源评论数
                    Object commentCount=jsonDataObject1.get("reply_count");
                    if(!StringUtils.isEmpty(commentCount)) {
                        sourceCommentCount=Integer.valueOf(commentCount.toString());
                    }
                    NewsDO newsDO = newsCoreManager.buildNewsDO(
                            newsId, dataKey, title.toString(), newsHot,
                            newsUrl, shareUrl, source.toString(), newsSourceUrl.toString(),
                            NewsDO.NEWS_TYPE_FINANCE,
                            NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                            keywords.toString(), banComment,
                            newsAbstract.toString(), displayTime,
                            videoCount, 0, null,
                            null, sourceCommentCount, 0);
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
            List<Selectable> nodesContent=html.xpath("//div[@class='article__bd__detail']").nodes();
            String content=nodesContent.get(0).toString();

            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }
            NewsDO newsDO=cacheNewsDO.get(dataKey);
            List<Selectable> imgNodes=nodesContent.get(0).xpath("//img/@src").nodes();
            //默认封面图有1张
            Integer imageCount =0;
            List<NewsImageDO> newsImageDOList = new ArrayList<>();
            if(imgNodes.size()>0){
                imageCount=1;
                String imageUrl=imgNodes.get(0).toString();
                newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                        IDUtil.getNewID(), newsDO.getNewsId(),
                        imageUrl.toString(), 0, 0,
                        NewsImageDO.IMAGE_TYPE_MINI));
            }
            //展示方式
            Integer displayType = handleNewsDisplayType(imageCount);
            newsDO.setDisplayType(displayType);
            newsDO.setImageCount(imageCount);

            cacheNewsImageDO.put(newsDO.getDataKey(), newsImageDOList);
            xueqiuCrawlerPipeline.saveXueqiuNews(dataKey, cacheNewsDO, cacheNewsImageDO, content);
//            List<Selectable> nodesContent = content.xpath("//article[@class='s_card']").nodes();

        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
