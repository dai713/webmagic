package cn.mc.scheduler.crawler.hexun.com;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
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
@Component
public class HexunFinanceNewsCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(2000);

    private static final String URL = "http://m.hexun.com/finance.html";

    private static String href="http://m.hexun.com";

    private static Map<String, NewsDO> cacheNewsDO = Collections.EMPTY_MAP;

    private static Map<String, List<NewsImageDO>> cacheNewsImageDO = Collections.EMPTY_MAP;

    private Map cacheTitle = new HashMap();

    @Autowired
    private NewsCoreManager newsCoreManager;

    @Autowired
    private NewsImageCoreManager newsImageCoreManager;

    @Autowired
    private HexunFinanceNewsCrawlerPipeline hexunFinanceNewsCrawlerPipeline;

    @Override
    public Spider createCrawler() {
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
            Html html = page.getHtml();
            //获取列表
            List<Selectable> nodes = html.xpath("//div[@id='newsList']").nodes();
            List<Selectable> nodesLi=nodes.get(0).xpath("//ul[@class='news_list_1']//li").nodes();
            for(int i=0;i<nodesLi.size();i++){
                Selectable selectable=nodesLi.get(i);
                //获取标题
                String title=selectable.xpath("//div/text()").toString();
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
                //获取封面图
                String imageUrl = selectable.xpath("//img/@original").nodes().get(0).toString();
                if(StringUtils.isEmpty(imageUrl)){
                    continue;
                }
                //获取链接
                String newsSourceUrl = selectable.xpath("//a/@href").nodes().get(0).toString();
                String newsSource=selectable.xpath("//span[@class='fl']/text()").nodes().toString();

                String time=selectable.xpath("//span[@class='fr']/text()").nodes().get(0).toString();
                StringBuffer str=new StringBuffer();
                Calendar date = Calendar.getInstance();
                String year = String.valueOf(date.get(Calendar.YEAR));
                str.append(year).append("-").append(time).append(":00");
                //发布时间
                Date displayTime;
                try {
                    displayTime = DateUtil.parse(str.toString(), DateUtil.DATE_FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND2);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return;
                }
                newsSourceUrl=href+newsSourceUrl;
                String dataKey = EncryptUtil.encrypt(newsSourceUrl, "md5");
                String keywords="";
                //新闻Id
                Long newsId = IDUtil.getNewID();
                String newsUrl = "";
                String shareUrl = "";
                //是否禁止评论 0允许
                Integer banComment = 0;
                Integer newsHot = 0;
                Integer videoCount = 0;
                Integer sourceCommentCount = 0;
                List<NewsImageDO> newsImageDOList = new ArrayList<>();
                newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                        IDUtil.getNewID(), newsId,
                        imageUrl, 0, 0,
                        NewsImageDO.IMAGE_TYPE_MINI));
                Integer displayType = NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE;
                NewsDO newsDO = newsCoreManager.buildNewsDO(
                        newsId, dataKey, title, newsHot,
                        newsUrl, shareUrl, newsSource, newsSourceUrl,
                        NewsDO.NEWS_TYPE_FINANCE,
                        NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                        keywords, banComment,
                        "", displayTime,
                        videoCount, newsImageDOList.size(), null,
                        displayType, sourceCommentCount, 0);
                cacheNewsDO.put(newsDO.getDataKey(), newsDO);
                cacheNewsImageDO.put(newsDO.getDataKey(), newsImageDOList);
                //继续抓取详情页面
                Request request = new Request(newsSourceUrl);
                page.addTargetRequest(request);
            }
        }else{
            String url = String.valueOf(page.getUrl());
            Html html = page.getHtml();
            String dataKey = EncryptUtil.encrypt(url, "md5");
            List<Selectable> nodes = html.xpath("//article[@class='pbox']").nodes();
            String content = nodes.get(0).toString();
            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }
            // 获取新闻
            NewsDO newsDO = cacheNewsDO.get(dataKey);
            if(null==newsDO){
                return;
            }
            hexunFinanceNewsCrawlerPipeline.saveHexunNews(dataKey,newsDO, cacheNewsImageDO, content);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
