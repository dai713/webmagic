package cn.mc.scheduler.crawler.toutiao.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsContentPictureDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.mapper.NewsContentArticleMapper;
import cn.mc.scheduler.mapper.NewsContentPictureMapper;
import cn.mc.scheduler.mapper.NewsImageMapper;
import cn.mc.scheduler.mapper.NewsMapper;
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import cn.mc.scheduler.util.SchedulerUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 今日头条 - 搞笑栏目
 * 搞笑内容、图片信息等
 * @author daiqingwen
 * @date 2018/4/25 上午 9:20
 */
@Component
@Deprecated
public class ToutiaoFunnyCrawler extends BaseCrawler {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:dd");

    private static final Logger logger = LoggerFactory.getLogger(ToutiaoFunnyCrawler.class);

    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(1000);



    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsImageMapper newsImageMapper;
    @Autowired
    private NewsContentArticleMapper newsContentArticleMapper;
    @Autowired
    private NewsContentPictureMapper newsContentPictureMapper;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private SchedulerUtils schedulerUtils;
    @Autowired
    private NewsCoreManager newsCoreManager;


    // 存放文章列表信息
    private static List<NewsDO> LIST = new ArrayList<>();

    // 存放文章列表图片信息
    private static List<NewsImageDO> IMAGE_LIST = new ArrayList<>();

//    // 存放图文信息
//    private static List<NewsContentPictureDO> PICTURE_LIST = new ArrayList<>();
//
//    // 存放文章信息
//    private static List<NewsContentArticleDO> ARTICLE_LIST = new ArrayList<>();

    // 今日头条-推荐页 新闻列表请求url
    private static String LIST_URL = "http://lf.snssdk.com/api/news/feed/v80/?fp=GSTqFM4ZJYwOFlP5F2U1FYmSFYGO&version_code=6.6.5" +
            "&app_name=news_article&vid=16EC63C1-917A-448F-A9B7-CBC78DA014E9&device_id=7683998131&channel=App%20Store&resolution=750*1334" +
            "&aid=13&ab_version=304488,332200,271329,330813,297977,317498,318244,295827,239095,324283,170988,170989,325960,332075,325198," +
            "327237,281392,330630,297059,314417,276203,286212,313219,328613,328678,277769,329942,322322,327535,332561,317410,328672,327416," +
            "317077,280773,319957,326729,317210,321406,331720,301282,214069,302116,318436,331309,258356,247850,280449,281299,328218,330729," +
            "325618,330457,324093,328059,288416,290197,260650,326192,327183,324614,271178,326587,326524,326532&ab_feature=z1&openudid=68964f" +
            "7bfc6391f424eeabb2c4eaa2a1c783148b&pos=5pe9vb%252F88Pzt0fLz%252BvTp6Pn4v72nvaysrrOkrqWrrKyvqq2oqaqvsZe9vb%252F88Pzt3vTp5L%252B9p7" +
            "2%252FeyoseAEueCUfv7GXvb2%252F%252FPD87dH86fTp6Pn4v72nva%252Bvs6ior6msrKWkpaitrK6ksZe9vb%252Fx%252FOn06ej5%252BL%252B9p72vr7OoqK%" +
            "252BprKylpKWorayupLGXvb2%252F8fLz%252BvTp6Pn4v72nvaysrrOkrqWrrKyvqq2oqaqvsZe9vb%252F%252B9Onkv72nvb97Kix4AS6%252Fl%252BA%253D" +
            "&idfv=16EC63C1-917A-448F-A9B7-CBC78DA014E9&ac=WIFI&os_version=10.3.3&ssmix=a&device_platform=iphone&iid=30058175430&ab_client=a1,f2,f7,e1" +
            "&device_type=iPhone%206S&idfa=F6F24C30-A099-42C1-A503-FF6F14440A49&LBS_status=authroize&category=funny&city=%E6%B7%B1%E5%9C%B3&concern_id=" +
            "6215497900768627201&count=20&cp=56AaDcF4DaED3q1&detail=1&image=1&language=zh-Hans-CN&last_refresh_sub_entrance_interval=2417&latitude=" +
            "22.55241189850139&list_count=26&loc_mode=1&loc_time=1524620782&longitude=113.9386112705472&min_behot_time=1524620671&refer=1&refresh_reason=2" +
            "&session_refresh_idx=2&st_time=2161&strict=0&support_rn=4&tt_from=tab&as=a2559d4df2fd2a5ebf3539&ts=1524621010 HTTP/1.1";

    // 初始化首页Cookie信息
    private Request setCookie(Request request) {
        request.addCookie("_ba", "BA0.2-20170301-51e32-7qosFmkl6jK8TG4cRnmr");
        request.addCookie("CNZZDATA1272189606", "1537112480-1523576421-%7C1524546011");
        request.addCookie("CNZZDATA1263676333", "1202688310-1524212849-%7C1524212849");
        request.addCookie("CNZZDATA1264530760", "1513180175-1514084936-%7C1517889313");
        request.addCookie("CNZZDATA1264561316", "1701079194-1511486263-%7C1511486263");
        request.addCookie("install_id", "30058175430");
        request.addCookie("ttreq", "1$c9da02f3cca83ab8d41ba39e08a55bde56368a25");
        request.addCookie("alert_coverage", "95");
        request.addCookie("sessionid", "379ddc42b8202f116d10d8b5bb41b7ee");
        request.addCookie("sid_guard", "379ddc42b8202f116d10d8b5bb41b7ee%7C1523959695%7C2592000%7CThu%2C+17-May-2018+10%3A08%3A15+GMT");
        request.addCookie("sid_tt", "379ddc42b8202f116d10d8b5bb41b7ee");
        request.addCookie("uid_tt", "7002c4204834d0276e6111a0da9ff6d5");
        request.addCookie("UM_distinctid", "162bc6147d93b4-079e0320e340c7-1773185e-3d10d-162bc6147da799");
        request.addCookie("_ga", "GA1.2.1730464358.1484787036");
        request.addCookie("login_flag", "fed9b272cc7d41b477bdd8cab6302d1b");
        request.addCookie("odin_tt", "06f0bfedccd961912728241af6c13671c03d88968321516ae002bc5def784930d83a10910a904e601d9377cfb9a3a8c1");
        request.addCookie("uuid", "w:d6e0b3c35d9544fea83aebb5d6f3bd39");
        return request;
    }

    @Override
    public Spider createCrawler() {
        Request request = new Request();
        Request resultReq = setCookie(request);
        resultReq.setUrl(LIST_URL);

        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(resultReq);
        return spider;
    }

    @Override
    public void process(Page page) {
        if (LIST_URL.equals(page.getUrl().toString())) {
            // 获取搞笑新闻列表信息
            this.getFunnyNewsList(page);
        } else {
            // 获取搞笑新闻详情信息
            this.getFunnyNewsDetail(page);
        }
    }

    /**
     * 获取搞笑新闻列表
     * @param page
     */
    private synchronized void getFunnyNewsList(Page page) {
        logger.info("爬取今日头条推荐新闻列表程序开始...");
        try {
            JSONObject jsonObject = JSON.parseObject(page.getRawText());
            // 获取今日头条推荐栏目新闻列表
            JSONArray data = jsonObject.getJSONArray("data");
            List<String> detail_url_list = new ArrayList<>();
            // 遍历新闻列表，获取每个新闻信息
            for (int i = 1 ; i < data.size() ; i++) {
                JSONObject obj = data.getJSONObject(i);
                JSONObject content = obj.getJSONObject("content");
                String date = SDF.format(new Date());
                Long newId = IDUtil.getNewID();
                Long item_id = content.getLong("item_id");
                if (StringUtils.isEmpty(item_id)) {
                    continue;
                }
                String source = content.getString("source");
                String label = (content.getString("label") != null ? content.getString("label") : "");
                if (source.contains("问答") || label.contains("广告")) {
                    continue;
                }
                // 判断是否为文章类型
                String schema = content.getString("schema");
                if (content.getInteger("video_style") != 2 && null == schema) {
                    String title = content.getString("title");
                    Integer hot = (content.getInteger("hot") != null ? content.getInteger("hot") : 0);
                    String display_url = content.getString("display_url");
                    String article_url = content.getString("article_url");
                    Integer comment_count = (content.getInteger("comment_count") != null ? content.getInteger("comment_count") : 0);
                    String keywords = schedulerUtils.subStringKeyword(content.getString("keywords"));
                    Integer ban_comment = (content.getInteger("ban_comment") != null ? content.getInteger("ban_comment") : 0);
                    String sub_abstract = content.getString("abstract");
                    String share_url = content.getString("share_url");
                    Integer share_count = (content.getInteger("share_count") != null ? content.getInteger("share_count") : 0);
                    Boolean has_image = (content.getBoolean("has_image") != null ? content.getBoolean("has_image") : false);
                    // 列表图片
                    JSONArray imageList = content.getJSONArray("image_list");
                    if (null != imageList && imageList.size() > 0 ) {
                        for (int j = 0 ; j < imageList.size(); j++) {
                            JSONObject imgObj = imageList.getJSONObject(j);
                            Integer imageType = NewsImageDO.IMAGE_TYPE_MINI;
                            if (imageList.size() == 1) {
                                imageType = NewsImageDO.IMAGE_TYPE_LARGE;
                            }
                            NewsImageDO imageDO = new NewsImageDO(IDUtil.getNewID(), newId, imgObj.getInteger("width"), imgObj.getInteger("height"), imageType,
                                    imgObj.getString("url"), NewsImageDO.STATUS_NORMAL);
                            IMAGE_LIST.add(imageDO);
                        }
                    }
                    // 获取列表信息
                    NewsDO newsDO =  newsCoreManager.buildNewsDO(newId, item_id.toString(), title, hot, display_url, share_url, source, article_url,
                            NewsDO.NEWS_TYPE_JOKE, NewsDO.CONTENT_TYPE_LARGE_PIC, (keywords == null ? "" : keywords), ban_comment,
                            sub_abstract, SDF.parse(date), 0, (has_image ? 1 : 0), SDF.parse(date),
                            NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE, comment_count, comment_count);
                    // 获取item_id 和 group_id 用于拼接详情url
                    LIST.add(newsDO);
                    // 今日头条-新闻详情请求url
                    String DETAIL_URL = "http://a3.pstatp.com/article/content/19/2/";
                    StringBuilder sb = new StringBuilder(DETAIL_URL);
                    sb.append(item_id.toString()).append("/").append(item_id.toString()).append("/1/0/");
                    detail_url_list.add(sb.toString());
                }

            }
            // 新增新闻列表
            newsMapper.insertNews(LIST);
            // 新增新闻列表图片
            newsImageMapper.insertNewsImage(IMAGE_LIST);
//            JSONArray ja = JSONArray.parseArray(JSON.toJSONString(list));
            page.addTargetRequests(detail_url_list);
        } catch (Exception e){
            logger.error("爬取今日头条推荐新闻列表失败：" + e);
            e.printStackTrace();
        }
    }


    /**
     * 获取搞笑新闻详情，包含图片和文章
     * @param page
     */
    private void getFunnyNewsDetail(Page page) {
        try {
            JSONObject jsonObject = JSON.parseObject(page.getRawText());
            JSONObject data = jsonObject.getJSONObject("data");
            String item_id = data.getLong("item_id").toString();
            String content = data.getString("content");
            JSONArray array = data.getJSONArray("gallery");
            // 判断是否为图文类型
            if (null != array && array.size() > 0 ) {
                // 处理图文类型新闻
                for (int i = 0 ; i < array.size() ; i++) {
                    JSONObject imageObj = array.getJSONObject(i);
                    JSONObject img = imageObj.getJSONObject("sub_image");
                    String sub_abstract = imageObj.getString("sub_abstract");
                    // 上传图片至阿里云
                    String url = aliyunOSSClientUtil.uploadPicture(img.getString("url"));
                    NewsContentPictureDO pictureDO = new NewsContentPictureDO(IDUtil.getNewID(), 0L, (i+1), url, img.getInteger("width"),
                            img.getInteger("height"), sub_abstract, NewsContentPictureDO.STATUS_NORMAL);
                    for (NewsDO newsDO : LIST) {
                        if (item_id.equals(newsDO.getDataKey())) {
                            pictureDO.setNewsId(newsDO.getNewsId());
                            newsDO.setDisplayType(NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE);
                            newsDO.setContentType(NewsDO.CONTENT_TYPE_LARGE_PIC);
                            break;
                        }
                    }
//                    picture_list.add(pictureDO);
                    // 新增大图新闻
                    newsContentPictureMapper.insertPicture(pictureDO);
                }
            } else {
                // 处理文章类型新闻
                NewsContentArticleDO articleDo = new NewsContentArticleDO(IDUtil.getNewID(), 0L, content, NewsContentArticleDO.ARTICLE_TYPE_HTML);
                for (NewsDO newsDO : LIST) {
                    if (item_id.equals(newsDO.getDataKey())) {
                        articleDo.setNewsId(newsDO.getNewsId());
                        break;
                    }
                }
//                article_list.add(articleDo);
                // 新增文章内容
                newsContentArticleMapper.insertArticle(articleDo);
            }
//            JSONArray ja = JSONArray.parseArray(JSON.toJSONString(picture_list));
        } catch (Exception e) {
            logger.error("爬取今日头条推荐新闻详情失败：" + e);
            e.printStackTrace();
        }

    }

    @Override
    public Site getSite() {
        return SITE;
    }
}
