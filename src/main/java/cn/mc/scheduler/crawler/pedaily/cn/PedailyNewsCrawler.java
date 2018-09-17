package cn.mc.scheduler.crawler.pedaily.cn;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class PedailyNewsCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static final String URL = "https://m.pedaily.cn/news";

    private Map<String, NewsDO> cacheNewsDO = Maps.newHashMap();

    private Map<String, List<NewsImageDO>> cacheNewsImageDO = Maps.newHashMap();

    @Autowired
    private NewsCoreManager newsCoreManager;

    @Autowired
    private NewsImageCoreManager newsImageCoreManager;

    @Autowired
    private PedailyNewsCrawlerPipeline pedailyNewsCrawlerPipeline;

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
            List<Selectable> nodes = html.xpath("//div[@class='news-list']").nodes();
            if(!CollectionUtils.isEmpty(nodes)){
                //获取li对象参数
                List<Selectable> contentNodeList =  nodes.get(0).xpath("//li").nodes();
                for(Selectable contentNode:contentNodeList){
                    //获取封面图
                    String imageUrl=contentNode.xpath("//img/@data-src").nodes().get(0).toString();
                    if(StringUtils.isEmpty(imageUrl)){
                        continue;
                    }
                    //获取标题
                    String title=contentNode.xpath("//h3/text()").nodes().get(0).toString();
                    //获取链接
                    String newsSourceUrl=contentNode.xpath("//a/@href").toString();
                    String dataKey= EncryptUtil.encrypt(newsSourceUrl, "md5");
                    //获取来源  来源暂时没有 在详情中抓取
                    String source="";
                    String keywords;
                    //获取关键字
                    List<Selectable> keywordsList=contentNode.xpath("//span/text()").nodes();
                    if(keywordsList.size()>0){
                        keywords=keywordsList.get(0).toString();
                    }else {
                        keywords=title;
                    }
                    //新闻Id
                    Long newsId = IDUtil.getNewID();
                    String newsUrl = "";
                    String shareUrl = "";
                    //是否禁止评论 0允许
                    Integer banComment=0;
                    Integer newsHot = 0;
                    Integer videoCount = 0;
                    Integer sourceCommentCount=0;
                    List<NewsImageDO> newsImageDOList = new ArrayList<>();
                    newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                            IDUtil.getNewID(), newsId,
                            imageUrl, 0, 0,
                            NewsImageDO.IMAGE_TYPE_MINI));
                    Integer displayType=NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE;
                    NewsDO newsDO = newsCoreManager.buildNewsDO(
                            newsId, dataKey, title, newsHot,
                            newsUrl, shareUrl, source, newsSourceUrl,
                            NewsDO.NEWS_TYPE_HEADLINE,
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
            Html html = page.getHtml();
            String dataKey = EncryptUtil.encrypt(url, "md5");
            List<Selectable> nodes = html.xpath("//div[@class='news-content']").nodes();

            String content = nodes.get(0).toString();
            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }

            //把所有的a标签的换成div
            content=content.replaceAll("<a","<div");
            content=content.replaceAll("a>","div>");
            //获取文章发布时间
            List<Selectable> timeNodes = html.xpath("//span[@class='date']/text()").nodes();
            String time =timeNodes.get(0).toString().trim();
            time=time+":00";
            //发布时间
            Date displayTime = null;
            try {
                displayTime=DateUtil.parse(time,DateUtil.DATE_FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND2);
            }catch (Exception ex){
                ex.printStackTrace();
            }
            //获取文章来源作者
            List<Selectable> sourceNodes= html.xpath("//span[@class='author']/text()").nodes();
            String source =sourceNodes.get(0).toString().trim();
            // 获取新闻
            NewsDO newsDO = cacheNewsDO.get(dataKey);
            if(null!=newsDO){
                newsDO.setDisplayTime(displayTime);
                newsDO.setNewsSource(source);
                pedailyNewsCrawlerPipeline.savePedailyNews(dataKey,newsDO,cacheNewsImageDO,content);
            }

        }

    }
    @Override
    public Site getSite() {
        return site;
    }
}
