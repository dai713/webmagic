package cn.mc.scheduler.crawler.wangyi.com;

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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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

@Component
public class WangYiSprotCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static  String URL ="https://3g.163.com/touch/reconstruct/article/list/" +
            "BA8E6OEOwangning/0-20.html";

    private Map<String, NewsDO> cacheNewsDO = Maps.newHashMap();

    private Map<String, List<NewsImageDO>> cacheNewsImageDO = Maps.newHashMap();

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private WangYiSportPipeline wangyiSportPipeline;

    @Override
    public Spider createCrawler() {
        Request request = new Request(URL);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public synchronized void process(Page page) {
        if (URL.contains(page.getUrl().toString())) {
            String jsonReturn=page.getRawText();
            String jsonStr=jsonReturn.replaceAll("artiList","");
            jsonStr=jsonStr.substring(jsonStr.indexOf("(")+1,jsonStr.lastIndexOf(")"));
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            JSONArray dataJSONOArray = (JSONArray) jsonObject.get("BA8E6OEOwangning");

            for (int i=0;i<dataJSONOArray.size();i++) {
                //????????????
                JSONObject jsonDataObject = (JSONObject) dataJSONOArray.get(i);
                //????????????????????? ???????????????
                Object skipURL=jsonDataObject.get("skipURL");
                if(!StringUtils.isEmpty(skipURL)){
                    continue;
                }
                //?????? ??????url
                Object newsSourceUrl = jsonDataObject.get("url");
                if(StringUtils.isEmpty(newsSourceUrl)){
                    continue;
                }
                //??????
                Object title = jsonDataObject.get("title");
                if(title.toString().equals("????????????")){
                    continue;
                }
                //dataKey
                String dataKey = EncryptUtil.encrypt(String.valueOf(newsSourceUrl), "md5");
                //??????
                Object newsAbstract = jsonDataObject.get("digest");
                //??????Id
                Long newsId = IDUtil.getNewID();
                //??????????????????1???
                Integer imageCount =1;
                List<NewsImageDO> newsImageDOList = new ArrayList<>();
                //?????????
                Object imgsrc = jsonDataObject.get("imgsrc");
                if(!StringUtils.isEmpty(imgsrc)) {
                    int width = 0;
                    int height = 0;
                    newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                            IDUtil.getNewID(), newsId,
                            imgsrc.toString(), width, height,
                            NewsImageDO.IMAGE_TYPE_MINI));
                }else {
                    continue;
                }
                //????????????
                Integer displayType = handleNewsDisplayType(imageCount);
                //?????????????????? 0??????
                Integer banComment=0;
                //?????????
                String keywords = String.valueOf(jsonDataObject.get("source"))
                        .replaceAll("\\[", "")
                        .replaceAll("]", "")
                        .replaceAll("\"", "");
                Integer newsHot = 0;
                //????????????
                Date displayTime = null;
                Object time= jsonDataObject.get("ptime");
                if(!StringUtils.isEmpty(time)){
                    displayTime = DateUtil.parse(
                            String.valueOf(time), "yyyy-MM-dd HH:mm:ss");
                }
                String newsUrl = "";
                String shareUrl = "";
                Integer videoCount = 0;
                Integer sourceCommentCount =0;
                //????????????
                Object commentCount=jsonDataObject.get("commentCount");
                if(!StringUtils.isEmpty(commentCount)) {
                    sourceCommentCount=Integer.valueOf(commentCount.toString());
                }
                //????????????
                Object source = jsonDataObject.get("source");
                NewsDO newsDO = newsCoreManager.buildNewsDO(
                        newsId, dataKey, title.toString(), newsHot,
                        newsUrl, shareUrl, source.toString(), newsSourceUrl.toString(),
                        NewsDO.NEWS_TYPE_SPORTS,
                        NewsDO.CONTENT_TYPE_IMAGE_TEXT,
                        keywords, banComment,
                        newsAbstract.toString(), displayTime,
                        videoCount, imageCount, null,
                        displayType, sourceCommentCount, 0);
                cacheNewsDO.put(newsDO.getDataKey(), newsDO);
                cacheNewsImageDO.put(newsDO.getDataKey(), newsImageDOList);
                //????????????????????????
                Request request = new Request(newsSourceUrl.toString());
                page.addTargetRequest(request);
            }
        }else{
            String url= String.valueOf(page.getUrl());
            String dataKey = EncryptUtil.encrypt(url, "md5");
            Html html = page.getHtml();

            // ?????????????????????????????????
            List<Selectable> nodes =html.xpath("//div[@class='content']").nodes();
            Selectable articleNode = nodes.get(0);
            //???????????????????????????????????????
            List<Selectable> voideNodes = articleNode.xpath("//video").nodes();
            for (Selectable video :voideNodes) {
                Element elements= HtmlNodeUtil.getElements((HtmlNode) video).get(0);
                elements.remove();
                return;
            }

            String content = html.xpath("//div[@class='content']").toString();
            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)){
                return;
            }

            // ????????????
            content = content.replaceAll("data-src","src");
            // ?????????
            wangyiSportPipeline.saveNews(dataKey, cacheNewsDO, cacheNewsImageDO, content);
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
