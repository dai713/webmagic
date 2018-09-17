package cn.mc.scheduler.crawler.cnfol.com;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.crawler.chinaventure.com.cn.TouZhongNewsCrawlerPipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.util.*;

/**
 * 中金在线（财经）
 *
 * @author xl
 * @date 2018/7/23 下午 15:46
 */
@Component
public class CnfolFinanceNewsCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static final String URL = "http://3g.cnfol.com/news/";

    private static Map<String, NewsDO> cacheNewsDO = Collections.EMPTY_MAP;

    private static Map<String, List<NewsImageDO>> cacheNewsImageDO = Collections.EMPTY_MAP;

    @Autowired
    private NewsCoreManager newsCoreManager;

    @Autowired
    private NewsImageCoreManager newsImageCoreManager;

    @Autowired
    private CnfoNewsCrawlerPipeline cnfoNewsCrawlerPipeline;

    @Override
    public BaseSpider createCrawler() {
        cacheNewsDO = new LinkedHashMap<>();
        cacheNewsImageDO = new LinkedHashMap<>();
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
            List<Selectable> nodes = html.xpath("//section[@class='landformsMap']").nodes();
            if (!CollectionUtils.isEmpty(nodes)) {
                //获取li对象参数
                List<Selectable> contentNodeList = nodes.get(0).xpath("//li").nodes();
                for (Selectable contentNode : contentNodeList) {
                    //获取封面图

                    List<Selectable> imgNodes= contentNode.xpath("//div[@class='pictureEssay']/a/img/@src").nodes();
                    String imageUrl;
                    if(imgNodes.size()>0){
                        imageUrl=imgNodes.get(0).toString();
                    }else{//丢弃
                        continue;
                    }

                    List<Selectable> titleNodes = contentNode.xpath("//div[@class='content']/a[@class='picTitle']/text()").nodes();
                    //获取标题
                    String title = titleNodes.get(0).toString();
                    //获取链接
                    String newsSourceUrl = contentNode.xpath("//div[@class='content']/a[@class='picTitle']/@href").toString();
                    String dataKey = EncryptUtil.encrypt(newsSourceUrl, "md5");
                    //获取关键字
                    String keywords = contentNode.xpath("//div[@class='talk']/a[@class='talkTit']/text()").toString();
//                    String keywords=listKeywordNodes.get(0).toString();

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
                            newsUrl, shareUrl, "", newsSourceUrl,
                            NewsDO.NEWS_TYPE_TECHNOLOGY,
                            NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                            keywords, banComment,
                            "", null,
                            videoCount, newsImageDOList.size(), null,
                            displayType, sourceCommentCount, 0);
                    cacheNewsDO.put(newsDO.getDataKey(), newsDO);
                    cacheNewsImageDO.put(newsDO.getDataKey(), newsImageDOList);
                    //继续抓取详情页面
                    Request request = new Request(newsSourceUrl);
                    page.addTargetRequest(request);
                }
            }
        } else {
            String url = String.valueOf(page.getUrl());
            Html html = page.getHtml();
            String dataKey = EncryptUtil.encrypt(url, "md5");
            List<Selectable> nodes = html.xpath("//section[@id='ArtHideArea']").nodes();

            String content = nodes.get(0).toString();
            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }

            //把所有的a标签的换成div
            content = content.replaceAll("<a", "<div");
            content = content.replaceAll("a>", "div>");
            content = content.replaceAll("\\\\n", "").replaceAll("\\\\", "");
            List<Selectable> timeNodes = html.xpath("//div[@class='ArtTags']/p/time/text()").nodes();

            String time = timeNodes.get(0).toString().trim()+" "+DateUtil.getCurrentMinutesandSeconds();
            List<Selectable> sourceNodes = html.xpath("//div[@class='ArtTags']/p/span/span[@class='Mr10']/text()").nodes();
            if(sourceNodes.size()<=0){
                sourceNodes = html.xpath("//div[@class='ArtTags']/p/span/a/text()").nodes();
            }
            String source="中金在线";
            if(sourceNodes.size()>0){
                source = sourceNodes.get(0).toString();
            }
            //发布时间
            Date displayTime;
            try {
                displayTime = DateUtil.parse(time, DateUtil.DATE_FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND2);
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
            // 获取新闻
            NewsDO newsDO = cacheNewsDO.get(dataKey);
            if (null != newsDO) {
                newsDO.setNewsSource(source);
                newsDO.setDisplayTime(displayTime);
                cnfoNewsCrawlerPipeline.saveCnfoNews(dataKey, newsDO, cacheNewsImageDO, content);
            }

        }

    }

    @Override
    public Site getSite() {
        return site;
    }
}
