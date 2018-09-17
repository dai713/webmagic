package cn.mc.scheduler.crawler.mydrivers.com;

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
import com.google.common.collect.Maps;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Selectable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 快科技新闻
 *
 * @author xl
 * @date 2018/7/23 下午 15:46
 */
@Component
public class FastTechnologyNewsCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static final String URL = "https://m.mydrivers.com/";

    private Map<String, NewsDO> cacheNewsDO = Maps.newHashMap();

    private Map<String, List<NewsImageDO>> cacheNewsImageDO = Maps.newHashMap();

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private FastTechnologyNewsCrawlerPipeline fastTechnologyNewsCrawlerPipeline;

    @Override
    public BaseSpider createCrawler() {
        Request request = new Request(URL);
        request.addHeader(HEADER_USER_AGENT_KEY, USER_AGENT_IPHONE_OS);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public synchronized void process(Page page) {

        if (URL.contains(page.getUrl().toString())) {
            Html html = page.getHtml();
            //获取列表
            List<Selectable> nodes = html.xpath("//div[@class='newslist']").nodes();
            if(!CollectionUtils.isEmpty(nodes)){
                //获取dataId参数
                List<Selectable> contentNodeList =  nodes.get(0).xpath("//li[@data-id]").nodes();
                for(Selectable contentNode:contentNodeList){

                    List<Selectable> spanNodes = contentNode.xpath("//span[@class='newst']").nodes();
                    String title;
                    String newsSourceUrl;
                    if(spanNodes.size()>0){
                        //获取标题
                        title=spanNodes.get(0).xpath("//a/text()").nodes().get(0).toString();
                        newsSourceUrl=spanNodes.get(0).xpath("//a/@href").toString();
                    }else{
                        List<Selectable> thNodes = contentNode.xpath("//span[@class='newstnopic']").nodes();
                        title=thNodes.get(0).xpath("//a/text()").nodes().get(0).toString();
                        newsSourceUrl=thNodes.get(0).xpath("//a/@href").toString();
                    }
                    if(StringUtils.isEmpty(title)){
                        continue;
                    }
                    newsSourceUrl=URL+newsSourceUrl;
                    String dataKey=EncryptUtil.encrypt(newsSourceUrl, "md5");
                    //获取来源
                    String source=contentNode.xpath("//li[@class='tname']/text()").nodes().get(0).toString();
                    source=source.trim().replaceAll("｜","");
                    //新闻Id
                    Long newsId = IDUtil.getNewID();
                    String newsUrl = "";
                    String shareUrl = "";
                    //是否禁止评论 0允许
                    Integer banComment=0;
                    Integer newsHot = 0;
                    String keywords = title;
                    Integer videoCount = 0;
                    Integer sourceCommentCount=0;
                    //获取评论数
                    String commentCount =contentNode.xpath("//div[@class='tpinglun']").xpath("//a/text()").toString();
                    if(!StringUtils.isEmpty(commentCount)){
                        sourceCommentCount=Integer.parseInt(commentCount);
                    }
                    //获取封面图
                    List<Selectable> imageUrlList=contentNode.xpath("//img").nodes();
                    List<NewsImageDO> newsImageDOList = new ArrayList<>();
                    for(Selectable selectable:imageUrlList){
                        String imageUrl=selectable.xpath("//img/@src").toString();
                        newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                                IDUtil.getNewID(), newsId,
                                imageUrl, 240, 180,
                                NewsImageDO.IMAGE_TYPE_MINI));
                    }
                    Integer displayType = handleNewsDisplayType(newsImageDOList.size());
                    NewsDO newsDO = newsCoreManager.buildNewsDO(
                            newsId, dataKey, title, newsHot,
                            newsUrl, shareUrl, source, newsSourceUrl,
                            NewsDO.NEWS_TYPE_TECHNOLOGY,
                            NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                            keywords, banComment,
                            "", null,
                            videoCount, 1, null,
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
            String dataKey = EncryptUtil.encrypt(url, "md5");
            List<Selectable> nodes = html.xpath("//div[@id='content']").nodes();
            //收藏那段代码去掉
            List<Selectable> shoucangNodes = nodes.get(0).xpath("//a[@id='a_shoucang']").nodes();
            for (Selectable shoucang : shoucangNodes) {
                Element elements = HtmlNodeUtil.getElements((HtmlNode) shoucang).get(0);
                elements.remove();
            }
            List<Selectable> shoucangDivNodes = nodes.get(0).xpath("//div[@id='a_shoucang']").nodes();
            for (Selectable shoucangDiv : shoucangDivNodes) {
                Element elements = HtmlNodeUtil.getElements((HtmlNode) shoucangDiv).get(0);
                elements.remove();
            }
            String content = nodes.get(0).toString();
            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }

            //把所有的a标签的换成div
            content=content.replaceAll("<a","<div");
            content=content.replaceAll("a>","div>");
            List<Selectable> timeNodes = html.xpath("//div[@class='news_t1']").nodes();
            List<Selectable> liNodes=timeNodes.get(0).xpath("//li").nodes();
            String time=liNodes.get(2).xpath("//li/text()").toString().trim();
            time=time.replaceAll("年","-");
            time=time.replaceAll("月","-");
            time=time.replaceAll("日","");
            time=time+":00";
            //发布时间
            Date displayTime = null;
            try {
                displayTime= DateUtil.parse(time,DateUtil.DATE_FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND2);
            }catch (Exception ex){
                ex.printStackTrace();
            }
            // 获取新闻
            NewsDO newsDO = cacheNewsDO.get(dataKey);
            if(null!=newsDO){
                newsDO.setDisplayTime(displayTime);
                fastTechnologyNewsCrawlerPipeline.saveFastTechnologyNews(dataKey,newsDO,cacheNewsImageDO,content);
            }

        }

    }
    @Override
    public Site getSite() {
        return site;
    }
}
