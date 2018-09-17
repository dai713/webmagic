package cn.mc.scheduler.crawler.santaihu.com;

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
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Selectable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 三泰虎新闻
 *
 * @author xl
 * @date 2018/8/6 下午 15:46
 */
@Component
public class SantaihuNewsCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static final String URL = "http://www.santaihu.com/";

    private Map<String, NewsDO> cacheNewsDO = Maps.newHashMap();

    private Map<String, List<NewsImageDO>> cacheNewsImageDO = Maps.newHashMap();

    @Autowired
    private NewsCoreManager newsCoreManager;

    @Autowired
    private NewsImageCoreManager newsImageCoreManager;

    @Autowired
    private SantaihuNewsCrawlerPipeline santaihuNewsCrawlerPipeline;

    @Override
    public Spider createCrawler() {
        Request request = new Request(URL);
        request.addHeader(HEADER_USER_AGENT_KEY, USER_AGENT_IPHONE_OS);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public void process(Page page) {
        if (URL.contains(page.getUrl().toString())) {
            Html html = page.getHtml();
            //获取列表
            List<Selectable> nodes = html.xpath("//article[@class='excerpt']").nodes();
            for(Selectable contentNode:nodes){
                //获取标题
                String title=contentNode.xpath("//h2/a/text()").nodes().get(0).toString();

                //获取封面图
                String imageUrl=contentNode.xpath("//a[@class='thumbnail']/img/@src").nodes().get(0).toString();
                if(StringUtils.isEmpty(imageUrl)){
                    continue;
                }
                //获取连接
                String newsSourceUrl=contentNode.xpath("//a[@class='thumbnail']/@href").toString();
                //如果连接包含php则是vip内容 直接过滤掉
                if(newsSourceUrl.contains("php")){
                    continue;
                }
                String dataKey= EncryptUtil.encrypt(newsSourceUrl, "md5");
                //获取来源  来源暂时没有 在详情中抓取
                String source="";
                StringBuffer keywords=new StringBuffer();
                //获取关键字
                List<Selectable> keywordsList=contentNode.xpath("//span[@class='post-tags']/a/text()").nodes();
                if(keywordsList.size()>0){
                    for(Selectable selectable:keywordsList){
                        keywords.append(selectable.toString()).append(",");
                    }
                }else {
                    keywords.append(title);
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
                        NewsDO.NEWS_TYPE_TECHNOLOGY,
                        NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                        keywords.toString(), banComment,
                        "", null,
                        videoCount, 1, null,
                        displayType, sourceCommentCount, 0);
                cacheNewsDO.put(newsDO.getDataKey(), newsDO);
                cacheNewsImageDO.put(newsDO.getDataKey(), newsImageDOList);
                //继续抓取详情页面
                Request request = new Request(newsSourceUrl);
                page.addTargetRequest(request);
            }
        }else {
            String url= String.valueOf(page.getUrl());
            Html html = page.getHtml();
            String dataKey = EncryptUtil.encrypt(url, "md5");

            List<Selectable> nodes = html.xpath("//article[@class='article-content']").nodes();
            //隐藏的图片删除掉
            List<Selectable> styleNodes=nodes.get(0).xpath("//div[@class='article-pic']").nodes();
            for (Selectable sNode : styleNodes) {
                Element elements = HtmlNodeUtil.getElements((HtmlNode) sNode).get(0);
                elements.remove();
                continue;
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
            String source ="信息来源：三泰虎";
            //获取文章发布时间
            List<Selectable> timeNodes = html.xpath("//div[@class='article-meta']/time/text()").nodes();
            String time =timeNodes.get(0).toString().trim();
            time=time+" 08:30:01";
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
                newsDO.setNewsSource(source);
                santaihuNewsCrawlerPipeline.saveSantaiNews(dataKey,newsDO,cacheNewsImageDO,content);
            }
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
