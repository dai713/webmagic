package cn.mc.scheduler.crawler.wx.qq.com;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.core.utils.MessageUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assertj.core.util.Lists;
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

/**
 * qq 娱乐
 *
 * @author sin
 * @time 2018/7/25 15:51
 */
@Component
public class QQEntertainmentCrawler extends BaseCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(QQMilitaryNewsCrawler.class);

    private Site site = Site.me().setRetryTimes(3).setSleepTime(300);

    private static final String URL_TEMPLATE = "https://pacaio.match.qq.com/xw/site" +
            "?uid=21312312&ext=ent&num=20&page={pageIndex}" +
            "&expIds=20180724A1XCNU%7C20180725A0IHJ3%7C20180724A0M4QB%7C20180724A19TRN%7C2018072" +
            "5A0NOQ3%7C20180725A0NMWO%7C20180725A0KIQL%7C20180725A06X1E%7C20180725A0JTAR%7C2018" +
            "0725A0BI2R%7C20180725A0H7YB%7C20180725A0ME8L%7C20180725A04SJG%7C20180725A0LQK5%7C" +
            "20180725A0GSNK%7C20180724A1YHXP%7C20180725A0K489%7C20180725A0UOLD%7C20180725A03T" +
            "JC%7C20180725A086Q1&callback=90";

    private List<String> URL_CACHE = Lists.newArrayList();
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
        int size = 10;
        List<Request> requests = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            String url = MessageUtil.doFormat2(URL_TEMPLATE,
                    ImmutableMap.of("pageIndex", i + 1));
            URL_CACHE.add(url);
            Request request = new Request(url);
            requests.add(request);
        }

        // create spider
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(requests.toArray(new Request[]{}));
        return spider;
    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();
        if (URL_CACHE.contains(url)) {
            String jsonReturn = page.getRawText();
            String jsonStr = jsonReturn.replaceAll("__jp0", "");
            jsonStr = jsonStr.substring(jsonStr.indexOf("(") + 1, jsonStr.lastIndexOf(")"));
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            JSONArray dataJSONOArray = (JSONArray) jsonObject.get("data");

            for (int i = 0; i < dataJSONOArray.size(); i++) {
                //获取对象
                JSONObject jsonDataObject = (JSONObject) dataJSONOArray.get(i);
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
                createTime = publishTime;

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
                        NewsDO.NEWS_TYPE_ENTERTAINMENT,
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

        }else {
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
}
