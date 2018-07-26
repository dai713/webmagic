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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QQTechnologyNewsCrawler extends BaseCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(QQTechnologyNewsCrawler.class);

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private static final String URL = "https://pacaio.match.qq.com/irs/rcd?" +
            "cid=56&ext=tech&token=c786875b8e04da17b24ea5e332745e0f" +
            "&num=20&page=0&expIds=&callback=__jp1";

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
            String jsonStr = jsonReturn.replaceAll("__jp1", "");
            jsonStr = jsonStr.substring(jsonStr.indexOf("(") + 1, jsonStr.lastIndexOf(")"));
            System.out.println(jsonStr);
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            JSONArray dataJSONOArray = (JSONArray) jsonObject.get("data");

            for (int i = 0; i < dataJSONOArray.size(); i++) {
                //获取对象
                JSONObject jsonDataObject = (JSONObject) dataJSONOArray.get(i);
                //获取腾讯的展示类型
                Integer disType = Integer.parseInt(jsonDataObject.get("article_type").toString());
                //来源 首先获取来源，确定手机显示的页面来源
                String newsSourceUrl;
                Integer contentType = NewsDO.CONTENT_TYPE_IMAGE_TEXT;
                newsSourceUrl = String.valueOf(jsonDataObject.get("url"));
                //标题
                String title = String.valueOf(jsonDataObject.get("title"));

                if (StringUtils.isEmpty(newsSourceUrl)) {
                    continue;
                }
                //描述
                String newsAbstract = String.valueOf(jsonDataObject.get("intro"));
                if (newsAbstract.length() > 50) {
                    newsAbstract = newsAbstract.substring(0, 50);
                }
                //新闻Id
                Long newsId = IDUtil.getNewID();
                List<NewsImageDO> newsImageDOList = new ArrayList<>();
                //封面图
                JSONObject irsImageJSONObject = jsonDataObject.getJSONObject("irs_imgs");

                JSONArray imagesJSONArray = null;
                String imageSize = null;
                if (irsImageJSONObject.containsKey("328X231")) {
                    imageSize = "328X231";
                    imagesJSONArray = irsImageJSONObject.getJSONArray("328X231");
                } else if (irsImageJSONObject.containsKey("276X194")) {
                    imageSize = "276X194";
                    imagesJSONArray = irsImageJSONObject.getJSONArray("227X148");
                } else if (irsImageJSONObject.containsKey("966X604")) {
                    imageSize = "966X604";
                    imagesJSONArray = irsImageJSONObject.getJSONArray("227X148");
                }

                // 这条新闻不要了
                if (CollectionUtils.isEmpty(imagesJSONArray)) {
                    return;
                }

                for (Object imageObject : imagesJSONArray) {
                    String[] imageSizeArray = imageSize.split("X");
                    int imageWidth = Integer.valueOf(imageSizeArray[0]);
                    int imageHeight = Integer.valueOf(imageSizeArray[1]);
                    String imageUrl = String.valueOf(imageObject);

                    newsImageDOList.add(
                            newsImageCoreManager.buildNewsImageDO(
                                    IDUtil.getNewID(), newsId,
                                    imageUrl, imageWidth, imageHeight,
                                    NewsImageDO.IMAGE_TYPE_MINI));
                }

                //如果没有图片则丢弃
                if (newsImageDOList.size() <= 0) {
                    continue;
                }
                //默认封面图有1张
                Integer imageCount = newsImageDOList.size();
                //展示方式
                Integer displayType = handleNewsDisplayType(imageCount);
                //是否禁止评论 0允许
                Integer banComment = 0;
                //关键词
                String keywords = String.valueOf(jsonDataObject.get("keywords"));
                Integer newsHot = 0;
                //发布时间
                Date displayTime = null;
                //创建时间
                Date createTime = null;
                //发布时间
                Date publishTime = DateUtil.parse(String.valueOf(
                        jsonDataObject.get("publish_time")),
                        "yyyy-MM-dd HH:mm:ss");

                displayTime = publishTime;
                displayTime = createTime;

                String newsUrl = "";
                String shareUrl = "";
                Integer videoCount = 0;
                Integer sourceCommentCount = Integer.parseInt(jsonDataObject.get("comment_num").toString());
                //信息来源
                String source = jsonDataObject.get("source").toString();
                String dataKey = EncryptUtil.encrypt(newsSourceUrl, "md5");
                NewsDO newsDO = newsCoreManager.buildNewsDO(
                        newsId, dataKey, title, newsHot,
                        newsUrl, shareUrl, source, newsSourceUrl,
                        NewsDO.NEWS_TYPE_TECHNOLOGY,
                        contentType,
                        keywords, banComment,
                        newsAbstract.toString(), displayTime,
                        videoCount, imageCount, createTime,
                        displayType, sourceCommentCount, 0);
                cacheNewsDO.put(newsDO.getDataKey(), newsDO);
                cacheNewsImageDO.put(newsDO.getDataKey(), newsImageDOList);
                //继续抓取详情页面
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
            } else {
                String rgex = "(contents:[^>]*ext_data)";
                content = getSubUtilSimple(content, rgex);
                if (!StringUtils.isEmpty(content)) {
                    content = asciiToNative(content);

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
                        LOGGER.error("Error: JSON 转换失败", ExceptionUtils.getStackTrace(e));
                    }

                    if (dataJSONOArray == null) {
                        return;
                    }

                    //文章内容
                    StringBuffer strText = new StringBuffer();

                    for (Object object : dataJSONOArray) {
                        JSONObject jsonDataObject = (JSONObject) object;
                        Integer type = Integer.parseInt(jsonDataObject.get("type").toString());
                        //如果类型为图片类型
                        String info = String.valueOf(jsonDataObject.get("value"));

                        //如果是图片
                        if (type == 2) {
                            strText.append("<p>").append("<img src=\"").append(info).append("\">").append("</p>");
                        }
                        //如果是中文文本内容
                        else if (type == 1) {
                            info = info.replaceAll("划重点", "");
                            //如果内容包含
                            if (info.contains("/n")) {
                                info = info.replaceAll("/n", "<p>");
                                //包含则用开始和结尾
                                String str[] = info.split("<p>");
                                for (String str1 : str) {
                                    strText.append("<p>").append(str1).append("</p>");
                                }
                            } else {//采用一段的
                                strText.append("<p>").append(info).append("</p>");
                            }
                        } else { //其他的视频还是什么 直接丢弃
                            return;
                        }

                    }


                    // 处理 json 标签
                    content = strText.toString();
                    content=content.replaceAll("\\?tp=webp","").trim();
                    //去保存
                    qqTechnologyNewsPipeline.saveQQTechnologyNews(dataKey, cacheNewsDO, cacheNewsImageDO, content);
                }

            }
        }
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static String getSubUtilSimple(String soap, String rgex) {
        Pattern pattern = Pattern.compile(rgex);// 匹配的模式
        Matcher m = pattern.matcher(soap);
        while (m.find()) {
            return m.group(1);
        }
        return "";
    }

    public static String asciiToNative(String asciicode) {
        String[] asciis = asciicode.split("\\\\u");
        String nativeValue = asciis[0];
        try {
            for (int i = 1; i < asciis.length; i++) {
                String code = asciis[i];
                nativeValue += (char) Integer.parseInt(code.substring(0, 4), 16);
                if (code.length() > 4) {
                    nativeValue += code.substring(4, code.length());
                }
            }
        } catch (NumberFormatException e) {
            LOGGER.error("Error: {}", ExceptionUtils.getStackTrace(e));
            return asciicode;
        }
        return nativeValue;
    }

    private Request addHeader(Request request) {
        request.addHeader("user-agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Mobile Safari/537.36");
        request.addHeader(":authority", "pacaio.match.qq.com");
        request.addHeader(":path", "/irs/rcd?cid=56&ext=tech&token=c786875b8e04da17b24ea5e332745e0f&num=20&page=0&expIds=&callback=__jp1");
        request.addHeader(":scheme", "https");
        request.addHeader("accept", "*/*");
        request.addHeader(":method", "GET");
        request.addHeader("accept-encoding", "gzip, deflate, br");
        request.addHeader("cookie", "pgv_info=ssid=s654476354; pgv_pvid=8439355782; pac_uid=0_5b1f974f58480; pgv_pvi=6053069824; pgv_si=s8642623488; webwx_data_ticket=gSdHmTDxlLNKZUmIuxGD41Zb");
        request.addHeader("accept-language", "zh-CN,zh;q=0.9");
        request.addHeader("referer", "https://xw.qq.com/m/tech/");
        return request;
    }
}
