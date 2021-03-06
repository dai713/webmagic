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
public class QQSportNewsCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static final String URL = "https://pacaio.match.qq.com/irs/rcd?" +
            "cid=56&ext=sports&token=c786875b8e04da17b24ea5e332745e0f" +
            "&num=30&page=0&expIds=&callback=__jp2";

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
    public synchronized void process(Page page) {
        if (URL.contains(page.getUrl().toString())) {
            String jsonReturn = page.getRawText();
            String jsonStr = jsonReturn.replaceAll("__jp2", "");
            jsonStr = jsonStr.substring(jsonStr.indexOf("(") + 1, jsonStr.lastIndexOf(")"));
//            System.out.println(jsonStr);
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
                        newsImageDOList.add(newsImageCoreManager.buildNewsImageDO(
                                IDUtil.getNewID(), newsId,
                                imageUrl, 0, 0,
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
                        NewsDO.NEWS_TYPE_SPORTS,
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
        } else {
            String url = String.valueOf(page.getUrl());
            String dataKey = EncryptUtil.encrypt(url, "md5");
            Html html = page.getHtml();
            String content = html.toString();
            if (StringUtils.isEmpty(content)
                    || StringUtils.isEmpty(dataKey)
                    || !cacheNewsDO.containsKey(dataKey)) {
                return;
            }

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

                JSONObject jsonObject = null;
                try {
                    jsonObject = (JSONObject) JSON.parse(content);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (jsonObject == null) {
                    return;
                }

                JSONArray dataJSONOArray = (JSONArray) jsonObject.get("contents");
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

                // ???????????? content
                content = strText.toString();
                content=content.replaceAll("\\?tp=webp","").trim();
                //?????????
                qqTechnologyNewsPipeline.saveQQTechnologyNews(dataKey, cacheNewsDO, cacheNewsImageDO, content);
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
        request.addHeader(":path", "/irs/rcd?cid=56&ext=sports&token=c786875b8e04da17b24ea5e332745e0f&num=20&page=0&expIds=&callback=__jp2");
        request.addHeader(":scheme", "https");
        request.addHeader("accept", "*/*");
        request.addHeader(":method", "GET");
        request.addHeader("accept-encoding", "gzip, deflate, br");
        request.addHeader("cookie", "pgv_pvid=8439355782; pac_uid=0_5b1f974f58480; pgv_pvi=6053069824; webwx_data_ticket=gSejGp87uGFHlYj6fHa3Nyd2; pgv_info=ssid=s3989913114");
        request.addHeader("accept-language", "zh-CN,zh;q=0.9");
        request.addHeader("referer", "https://xw.qq.com/m/sports/");
        return request;
    }
}
