package cn.mc.scheduler.crawler.i.ifeng.com;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.util.HtmlNodeUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Selectable;

import java.util.*;

/**
 * 凤凰体育新闻
 *
 * @author xl
 * @date 2018/7/23 下午 15:46
 */
@Component
public class FengHuangFinanceNewsCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(2000);

    private static String URL = "http://ifinance.ifeng.com/1_0/data.shtml?_=";

    private static String href = "http://ifinance.ifeng.com";

    private static Map<String, NewsDO> cacheNewsDO = Collections.EMPTY_MAP;

    private static Map<String, List<NewsImageDO>> cacheNewsImageDO = Collections.EMPTY_MAP;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private IfengNewsCrawlerPipeline ifengNewsCrawlerPipeline;

    private  String initUrl;
    @Override
    public Spider createCrawler() {
        cacheNewsDO = new LinkedHashMap<>();
        cacheNewsImageDO = new LinkedHashMap<>();
        StringBuffer urlStr = new StringBuffer(URL);
        urlStr.append(System.currentTimeMillis());
        initUrl=urlStr.toString();
        Request request = new Request(initUrl);
        request.addHeader(HEADER_USER_AGENT_KEY, "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
        return Spider.create(this).addRequest(request);
    }

    @Override
    public synchronized void process(Page page) {
        if (initUrl.contains(page.getUrl().toString())) {
            String jsonReturn = page.getRawText();
            jsonReturn = jsonReturn.replaceAll("getListDatacallback\\(", "");
            jsonReturn = jsonReturn.replaceAll("\\);", "");
            if (!StringUtils.isEmpty(jsonReturn)) {
                JSONArray dataJSONOArray = (JSONArray) JSON.parse(jsonReturn);
                for (int i = 0; i < dataJSONOArray.size(); i++) {
                    //获取对象
                    JSONObject jsonDataObject = (JSONObject) dataJSONOArray.get(i);

                    String title = jsonDataObject.get("title").toString();
                    List<NewsImageDO> newsImageDOList = new ArrayList<>();
                    //新闻Id
                    Long newsId = IDUtil.getNewID();
                    String newsSourceUrl = jsonDataObject.get("pageUrl").toString();
                    newsSourceUrl=href+newsSourceUrl;
                    String image = jsonDataObject.get("i_thumbnail").toString();
                    newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                            IDUtil.getNewID(), newsId,
                            image.toString(), 0, 0,
                            NewsImageDO.IMAGE_TYPE_MINI));
                    String dataKey = EncryptUtil.encrypt(newsSourceUrl.toString(), "md5");
                    //展示方式
                    Integer displayType = handleNewsDisplayType(newsImageDOList.size());
                    //是否禁止评论 0允许
                    Integer banComment = 0;
                    Integer newsHot = 0;
                    String newsUrl = "";
                    String shareUrl = "";
                    Integer videoCount = 0;
                    Integer sourceCommentCount = 0;
                    NewsDO newsDO = newsCoreManager.buildNewsDO(
                            newsId, dataKey, title.toString(), newsHot,
                            newsUrl, shareUrl, null, newsSourceUrl,
                            NewsDO.NEWS_TYPE_FINANCE,
                            NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                            null, banComment,
                            title, null,
                            videoCount, newsImageDOList.size(), null,
                            displayType, sourceCommentCount, 0);
                    cacheNewsDO.put(newsDO.getDataKey(), newsDO);
                    cacheNewsImageDO.put(newsDO.getDataKey(), newsImageDOList);
                    //继续抓取详情页面
                    Request request = new Request(newsSourceUrl.toString());
                    page.addTargetRequest(request);
                }
            }
        } else {
            String url = String.valueOf(page.getUrl());
            Html html = page.getHtml();
            String dataKey = EncryptUtil.encrypt(url, "md5");
            //获取内容
            List<Selectable> contentNodes = html.xpath("//div[@id='whole_content']").nodes();
            Selectable articleNode = contentNodes.get(0);
            String content = articleNode.toString();

            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }
            //获取时间和来源
            List<Selectable> timeSourceNodes = html.xpath("//div[@class='acTxtTit wrap_w94']").nodes();
            List<Selectable> soureTime = timeSourceNodes.get(0).xpath("//span/text()").nodes();
            //发布时间
            Date displayTime = null;
            //来源
            String source = null;
            if (!CollectionUtils.isEmpty(soureTime)) {
                String time = soureTime.get(0).toString() + " " + soureTime.get(1).toString() + ":00";
                try {
                    displayTime = DateUtil.parse(time, DateUtil.DATE_FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND2);
                    source = soureTime.get(2).toString();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                List<Selectable> ifengNodes = articleNode.xpath("//span[@class='ifengLogo']").nodes();
                for (Selectable ifeng : ifengNodes) {
                    Element elements = HtmlNodeUtil.getElements((HtmlNode) ifeng).get(0);
                    elements.remove();
                }
                content = articleNode.toString();
                NewsDO newsDO = cacheNewsDO.get(dataKey);
                if (null != newsDO) {
                    newsDO.setDisplayTime(displayTime);
                    newsDO.setNewsSource(source);
                    ifengNewsCrawlerPipeline.saveIfengTechnologyNews(dataKey, newsDO, cacheNewsImageDO, content);
                }

            }
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
