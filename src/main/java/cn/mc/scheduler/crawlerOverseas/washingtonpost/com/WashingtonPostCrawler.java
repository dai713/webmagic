package cn.mc.scheduler.crawlerOverseas.washingtonpost.com;

import cn.mc.core.dataObject.NewsContentOverseasArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsContentOverseasArticleCoreManager;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.crawlerOverseas.OverseasCrawlerPipeline;
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
 * 华盛顿邮报（科技类）新闻
 *
 * @author xl
 * @date 2018/08/28 下午 15:46
 */
@Component
public class WashingtonPostCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static final String URL = "https://www.washingtonpost.com/business/technology/?nid=top_nav_tech&noredirect=on&utm_term=.7ff4e81fa331";

    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsContentOverseasArticleCoreManager articleCoreManager;

    @Autowired
    private NewsCoreManager newsCoreManager;

    private static Map<String, NewsDO> cacheNewsDO = Collections.EMPTY_MAP;

    private static Map<String, List<NewsImageDO>> cacheNewsImageDO = Collections.EMPTY_MAP;

    private String title="华盛顿邮报";

    @Autowired
    private OverseasCrawlerPipeline overseasCrawlerPipeline;

    @Override
    public Spider createCrawler() {
        cacheNewsDO = new LinkedHashMap<>();
        cacheNewsImageDO = new LinkedHashMap<>();
        Request request = new Request(URL);
        request.addHeader(HEADER_USER_AGENT_KEY, USER_AGENT_ANDROID_OS);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public void process(Page page) {
        if (URL.contains(page.getUrl().toString())) {
            Html html = page.getHtml();
            //获取列表
            List<Selectable> nodes = html.xpath("//div[@itemprop='itemListElement']").nodes();
            for(Selectable contentNode:nodes){
                //获取标题
                List<Selectable> titleNodes=contentNode.xpath("//h3/a/text()").nodes();
                String title;
                if(titleNodes.size()>0){
                    title=titleNodes.get(0).toString();
                }else{//没有标题的丢弃
                    continue;
                }
                //获取源连接
                String newsSourceUrl=contentNode.xpath("//h3/a/@href").toString();

                String dataKey= EncryptUtil.encrypt(newsSourceUrl, "md5");
                //来源作者
                String source=title+contentNode.xpath("//li/a/span[@class='author']/text()").toString();
                String keywords="";
                //新闻Id
                Long newsId = IDUtil.getNewID();
                String newsUrl = "";
                String shareUrl = "";
                //是否禁止评论 0允许
                Integer banComment=0;
                Integer newsHot = 0;
                Integer videoCount = 0;
                Integer sourceCommentCount=0;
                String imageUrl=contentNode.xpath("//a/img/@data-low-res-src").toString();
                if(StringUtils.isEmpty(imageUrl)){
                    continue;
                }
                List<NewsImageDO> newsImageDOList = new ArrayList<>();
                newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                        IDUtil.getNewID(), newsId,
                        imageUrl, 0, 0,
                        NewsImageDO.IMAGE_TYPE_MINI));
                Integer displayType=NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE;

                NewsDO newsDO = newsCoreManager.buildNewsDO(
                        newsId, dataKey, title, newsHot,
                        newsUrl, shareUrl, source, newsSourceUrl,
                        NewsDO.NEWS_TYPE_TECHNOLOGY_OVERSEAS,
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

        }else {
            String url= String.valueOf(page.getUrl());
            Html html = page.getHtml();
            String dataKey = EncryptUtil.encrypt(url, "md5");
            List<Selectable> nodes = html.xpath("//article[@itemprop='articleBody']").nodes();
            String content = nodes.get(0).toString();
            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }
            Selectable contentNode=nodes.get(0).xpath("//span[@class='author-timestamp']/@content");
            //获取文章发布时间
            String time=contentNode.toString();

            time=time.replace("T"," ");
            //发布时间
            Date displayTime = null;
            try {
                displayTime= DateUtil.parse(time,DateUtil.DATE_FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE);
            }catch (Exception ex){
                ex.printStackTrace();
            }
            // 获取新闻
            NewsDO newsDO = cacheNewsDO.get(dataKey);
            newsDO.setDisplayTime(displayTime);
            //构建海外新闻内容结构
            NewsContentOverseasArticleDO newsContentOverseasArticleDO=articleCoreManager.buildOverseasArticleDO(IDUtil.getNewID(),newsDO.getNewsId(),content,newsDO.getTitle());
            List<NewsImageDO> newsImageDOList=cacheNewsImageDO.get(dataKey);
            //添加新闻
            overseasCrawlerPipeline.save(newsDO,newsImageDOList,newsContentOverseasArticleDO);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

}
