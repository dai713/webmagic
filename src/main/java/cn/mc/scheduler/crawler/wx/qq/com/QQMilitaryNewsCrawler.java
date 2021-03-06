package cn.mc.scheduler.crawler.wx.qq.com;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class QQMilitaryNewsCrawler extends BaseCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(QQMilitaryNewsCrawler.class);

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static final String URL = "https://pacaio.match.qq.com/xw/site?uid=0_5b1f974f" +
            "58480&ext=milite&num=20&page=0&expIds=&callback=__jp0";

    private Map<String, NewsDO> cacheNewsDO = Maps.newHashMap();

    private Map<String, List<NewsImageDO>> cacheNewsImageDO = Maps.newHashMap();

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private QQTechnologyNewsPipeline qqTechnologyNewsPipeline;

    @Override
    public Spider createCrawler() {
        Request request = new Request(URL);
        request = addHeader(request);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public void process(Page page) {
        if (URL.contains(page.getUrl().toString())) {
            String jsonReturn = page.getRawText();
            String jsonStr = jsonReturn.replaceAll("__jp0", "");
            jsonStr = jsonStr.substring(jsonStr.indexOf("(") + 1, jsonStr.lastIndexOf(")"));
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            JSONArray dataJSONOArray = (JSONArray) jsonObject.get("data");

            for (int i = 0; i < dataJSONOArray.size(); i++) {
                //????????????
                JSONObject jsonDataObject = (JSONObject) dataJSONOArray.get(i);
                //?????? ??????????????????????????????????????????????????????
                String newsSourceUrl;
                Integer contentType = NewsDO.CONTENT_TYPE_IMAGE_TEXT;
                newsSourceUrl = String.valueOf(jsonDataObject.get("url"));
                //??????
                String title = String.valueOf(jsonDataObject.get("title"));
                if (StringUtils.isEmpty(newsSourceUrl)) {
                    continue;
                }
                //??????
                String newsAbstract = String.valueOf(jsonDataObject.get("intro"));
                if (newsAbstract.length() > 50) {
                    newsAbstract = newsAbstract.substring(0, 50);
                }
                //??????Id
                Long newsId = IDUtil.getNewID();
                List<NewsImageDO> newsImageDOList = new ArrayList<>();
                //?????????
                JSONArray jsonArray = (JSONArray) jsonDataObject.get("multi_imgs");
                if (!CollectionUtils.isEmpty(jsonArray)) {
                    for (Object imageObject : jsonArray) {
                        String imageUrl = imageObject.toString();
                        if (StringUtils.isEmpty(imageUrl)) {
                            continue;
                        }
                        int width = 0;
                        int height = 0;
                        newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                                IDUtil.getNewID(), newsId,
                                imageUrl, width, height,
                                NewsImageDO.IMAGE_TYPE_MINI));
                    }
                } else {
                    continue;
                }
                //???????????????????????????
                if (newsImageDOList.size() <= 0) {
                    continue;
                }
                //??????????????????1???
                Integer imageCount = newsImageDOList.size();
                //????????????
                Integer displayType = handleNewsDisplayType(imageCount);
                //?????????????????? 0??????
                Integer banComment = 0;
                //?????????
                String keywords = String.valueOf(jsonDataObject.get("keywords"));
                Integer newsHot = 0;
                //????????????
                Date displayTime = null;
                //????????????
                Date createTime = null;
                //????????????
                Date publishTime = DateUtil.parse(String.valueOf(
                        jsonDataObject.get("publish_time")),
                        "yyyy-MM-dd HH:mm:ss");
                displayTime = publishTime;
                createTime = publishTime;

                String newsUrl = "";
                String shareUrl = "";
                Integer videoCount = 0;
                Integer sourceCommentCount = Integer.parseInt(jsonDataObject.get("comment_num").toString());
                //????????????
                String source = jsonDataObject.get("source").toString();
                String dataKey = EncryptUtil.encrypt(newsSourceUrl, "md5");
                NewsDO newsDO = newsCoreManager.buildNewsDO(
                        newsId, dataKey, title, newsHot,
                        newsUrl, shareUrl, source, newsSourceUrl,
                        NewsDO.NEWS_TYPE_MILITARY,
                        contentType,
                        keywords, banComment,
                        newsAbstract.toString(), displayTime,
                        videoCount, imageCount, createTime,
                        displayType, sourceCommentCount, 0);
                cacheNewsDO.put(newsDO.getDataKey(), newsDO);
                cacheNewsImageDO.put(newsDO.getDataKey(), newsImageDOList);
                //????????????????????????
                Request request = new Request(newsSourceUrl);
                page.addTargetRequest(request);
            }

        }else {
            String url = String.valueOf(page.getUrl());
            String dataKey = EncryptUtil.encrypt(url, "md5");
            Html html = page.getHtml();
            String content = html.toString();
            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
            } else {
                String rgex = "(contents:[^>]*ext_data)";
                content = QQTechnologyNewsCrawler.getSubUtilSimple(content, rgex);
                if (!StringUtils.isEmpty(content)) {
                    content = QQTechnologyNewsCrawler.asciiToNative(content);

                    content = content.replace("ext_data", "").trim();

                    content = content.replace("/", "").trim();
                    content = content.replace("contents", "{\"contents\"");
                    content = content.replace("],", "]}");
                    content = content.replaceAll("\\\\", "/");
                    content = content.replaceAll("/\"", "");

                    JSONArray dataJSONOArray = null;
                    try {
                        JSONObject jsonObject = JSON.parseObject(content);
                        dataJSONOArray = (JSONArray) jsonObject.get("contents");
                    } catch (Exception e) {
                        LOGGER.error("Error: {}", ExceptionUtils.getStackTrace(e));
                    }

                    if (dataJSONOArray == null) {
                        return;
                    }

                    //????????????
                    StringBuffer strText = new StringBuffer();

                    for (Object object : dataJSONOArray) {
                        JSONObject jsonDataObject = (JSONObject) object;
                        Integer type = Integer.parseInt(jsonDataObject.get("type").toString());
                        //???????????????????????????
                        String info = String.valueOf(jsonDataObject.get("value"));

                        //???????????????
                        if (type == 2) {
                            strText.append("<p>").append("<img src=\"").append(info).append("\">").append("</p>");
                        }
                        //???????????????????????????
                        else if (type == 1) {
                            info = info.replaceAll("?????????", "");
                            //??????????????????
                            if (info.contains("/n")) {
                                info = info.replaceAll("/n", "<p>");
                                //???????????????????????????
                                String str[] = info.split("<p>");
                                for (String str1 : str) {
                                    strText.append("<p>").append(str1).append("</p>");
                                }
                            } else {//???????????????
                                strText.append("<p>").append(info).append("</p>");
                            }
                        } else { //??????????????????????????? ????????????
                            return;
                        }

                    }
                    // ?????? json ??????
                    content = strText.toString();
                    content=content.replaceAll("\\?tp=webp","").trim();
                    //?????????
                    qqTechnologyNewsPipeline.saveQQTechnologyNews(dataKey, cacheNewsDO, cacheNewsImageDO, content);
                }
            }
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
    private Request addHeader(Request request) {
        request.addHeader("user-agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Mobile Safari/537.36");
        request.addHeader(":authority", "pacaio.match.qq.com");
        request.addHeader(":path", "/xw/site?uid=0_5b1f974f58480&ext=milite&num=20&page=0&expIds=&callback=__jp0");
        request.addHeader(":scheme", "https");
        request.addHeader("accept", "*/*");
        request.addHeader(":method", "GET");
        request.addHeader("accept-encoding", "gzip, deflate, br");
        request.addHeader("cookie", "pgv_pvid=8439355782; pac_uid=0_5b1f974f58480; pgv_pvi=6053069824; 3g_guest_id=-8735956084079923199; g_ut=3; webwx_data_ticket=gSeQAaQpP5M5ua2xYXRzVxxb; pgv_info=ssid=s9517285754");
        request.addHeader("referer", "https://xw.qq.com/m/mil/");
        return request;
    }

}
