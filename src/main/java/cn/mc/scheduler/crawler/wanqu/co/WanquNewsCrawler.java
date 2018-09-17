package cn.mc.scheduler.crawler.wanqu.co;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.crawler.pedaily.cn.PedailyNewsCrawlerPipeline;
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
 * 湾区日报
 *
 * @author xl
 * @date 2018/8/6 下午 15:46
 */
@Component
public class WanquNewsCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static final String URL = "https://wanqu.co/";

    private Map<String, NewsDO> cacheNewsDO = Maps.newHashMap();

    @Autowired
    private NewsCoreManager newsCoreManager;

    @Autowired
    private WanquNewsCrawlerPipeline wanquNewsCrawlerPipeline;

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
            List<Selectable> nodes = html.xpath("//li[@class='list-group-item']").nodes();
            for(Selectable contentNode:nodes){
                //获取标题
                List<Selectable> titleNodes=contentNode.xpath("//h2/a/text()").nodes();
                String title;
                if(titleNodes.size()>0){
                    title=titleNodes.get(0).toString();
                }else{//没有标题的丢弃
                    continue;
                }

                //获取连接
                String newsSourceUrl=contentNode.xpath("//h2/a/@href").toString();
                String dataKey= EncryptUtil.encrypt(newsSourceUrl, "md5");
                //获取来源  来源暂时没有 在详情中抓取
                String source="湾区日报";
                String keywords="湾区日报";

                //新闻Id
                Long newsId = IDUtil.getNewID();
                String newsUrl = "";
                String shareUrl = "";
                //是否禁止评论 0允许
                Integer banComment=0;
                Integer newsHot = 0;
                Integer videoCount = 0;
                Integer sourceCommentCount=0;
                Integer displayType=NewsDO.DISPLAY_TYPE_LINE_TEXT;
                NewsDO newsDO = newsCoreManager.buildNewsDO(
                        newsId, dataKey, title, newsHot,
                        newsUrl, shareUrl, source, newsSourceUrl,
                        NewsDO.NEWS_TYPE_TECHNOLOGY,
                        NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                        keywords.toString(), banComment,
                        "", null,
                        videoCount, 0, null,
                        displayType, sourceCommentCount, 0);
                cacheNewsDO.put(newsDO.getDataKey(), newsDO);
                //继续抓取详情页面
                Request request = new Request(newsSourceUrl);
                page.addTargetRequest(request);
            }
        }else {
            String url= String.valueOf(page.getUrl());
            Html html = page.getHtml();
            String dataKey = EncryptUtil.encrypt(url, "md5");

            List<Selectable> rowNodes = html.xpath("//div[@class='panel-body']").nodes();
            StringBuffer content=new StringBuffer();
            if(rowNodes.size()>0){
                List<Selectable> t1= rowNodes.get(0).xpath("div[@class='row']").nodes();
                if(t1.size()>0){
                    content.append(t1.get(0).toString());
                }
                List<Selectable> t2= rowNodes.get(0).xpath("div[@class='lead']").nodes();
                if(t2.size()>0){
                    content.append(t2.get(0).toString());
                }
            }
            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }

            //把所有的a标签的换成div
            //获取文章发布时间
            List<Selectable> timeNodes = html.xpath("//a[@rel='alternate']/text()").nodes();
            String time =timeNodes.get(1).toString().trim();
            time=time+" 08:30:01";
            //发布时间
            Date displayTime = null;
            try {
                displayTime= DateUtil.parse(time,DateUtil.DATE_FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND);
            }catch (Exception ex){
                ex.printStackTrace();
            }
            // 获取新闻
            NewsDO newsDO = cacheNewsDO.get(dataKey);
            if(null!=newsDO){
                newsDO.setDisplayTime(displayTime);
                wanquNewsCrawlerPipeline.saveWanquNews(dataKey,newsDO,content.toString());
            }
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
