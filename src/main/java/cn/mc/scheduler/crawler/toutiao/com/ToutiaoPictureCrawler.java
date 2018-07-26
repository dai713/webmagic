package cn.mc.scheduler.crawler.toutiao.com;

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
 * 今日头条 - 图片栏目
 * 轮播图信息
 * @author daiqingwen
 * @date 2018/4/24
 */
@Component
@Deprecated
public class ToutiaoPictureCrawler extends BaseCrawler {
    private static final Logger logger = LoggerFactory.getLogger(ToutiaoIndexInfoCrawler.class);

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:dd");

    private Site site = Site.me().setRetrySleepTime(3).setSleepTime(1000);

    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsImageMapper newsImageMapper;
    @Autowired
    private NewsContentPictureMapper newsContentPictureMapper;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private NewsCoreManager newsCoreManager;

    // 存放多个图片列表信息
    private static List<NewsDO> list = new ArrayList<>();

    // 存放列表图片信息
    private static List<NewsImageDO> image_list = new ArrayList<>();

//    // 存放图文信息
//    private static List<NewsContentPictureDO> picture_list = new ArrayList<>();

    // 图片列表请求url
    private static String LIST_URL = "http://lf.snssdk.com/api/news/feed/v80/?fp=GSTqFM4ZJYwOFlP5F2U1FYmSFYGO&version_code=6.6.5&app_name=news_article" +
            "&vid=16EC63C1-917A-448F-A9B7-CBC78DA014E9&device_id=7683998131&channel=App%20Store&resolution=750*1334&aid=13&ab_version=" +
            "304488,271329,330813,297977,317498,318244,295827,239095,324283,170988,170989,325960,320218,325198,327237,281392,330630,297059" +
            ",314417,276203,286212,313219,328613,328678,277769,329942,322322,327535,326537,330800,317410,330205,328672,327416,317077,280773" +
            ",330086,319957,326729,317210,321406,301282,214069,302116,318436,331309,258356,247850,280449,281299,328218,330729,325618,330457" +
            ",324093,328059,321414,288416,290197,260650,330903,326192,327183,324614,271178,326587,326524,326532&ab_feature=z1&openudid" +
            "=68964f7bfc6391f424eeabb2c4eaa2a1c783148b&pos=5pe9vb%252F88Pzt0fLz%252BvTp6Pn4v72nvaysrrOkqa2kr6WspayvqqmrsZe9vb%252F88Pzt3vTp5L%" +
            "252B9p72%252FeyoseAEueCUfv7GXvb2%252F%252FPD87dH86fTp6Pn4v72nva%252Bvs6ior6%252BspaWsra2tr6upsZe9vb%252Fx%252FOn06ej5%252BL%252B9" +
            "p72vr7OoqK%252BvrKWlrK2tra%252BrqbGXvb2%252F8fLz%252BvTp6Pn4v72nvaysrrOkqa2kr6WspayvqqmrsZe9vb%252F%252B9Onkv72nvb97Kix4AS6%" +
            "252Fl%252BA%253D&idfv=16EC63C1-917A-448F-A9B7-CBC78DA014E9&ac=WIFI&os_version=10.3.3&ssmix=a&device_platform=iphone&iid=30058175430" +
            "&ab_client=a1,f2,f7,e1&device_type=iPhone%206S&idfa=F6F24C30-A099-42C1-A503-FF6F14440A49&LBS_status=authroize&category=%E7%BB%84%E5" +
            "%9B%BE&city=%E6%B7%B1%E5%9C%B3&concern_id=&count=20&cp=59A9DcE286A0Fq1&detail=1&image=1&language=zh-Hans-CN&last_refresh_sub_entrance_interval" +
            "=1594&latitude=22.55221881000264&list_count=20&loc_mode=1&loc_time=1524532823&longitude=113.9409281812746&min_behot_time=1524532181&refer=1" +
            "&refresh_reason=2&session_refresh_idx=4&st_time=1233&strict=0&support_rn=4&tt_from=tab_tip&as=a255f84d6d707a1a3e9837&ts=1524533773 HTTP/1.1";

    // 初始化cookies信息
    private Request setCookies(Request request){
        request.addCookie("_ba","BA0.2-20170517-51e32-q1NnM9gF1bozU1vCJ6Ie");
        request.addCookie("install_id","30058175430");
        request.addCookie("ttreq","1$c9da02f3cca83ab8d41ba39e08a55bde56368a25");
        request.addCookie("qh[360]","1");
        request.addCookie("alert_coverage","20");
        request.addCookie("sessionid","379ddc42b8202f116d10d8b5bb41b7ee");
        request.addCookie("sid_guard","379ddc42b8202f116d10d8b5bb41b7ee%7C1523959695%7C2592000%7CThu%2C+17-May-2018+10%3A08%3A15+GMT");
        request.addCookie("sid_tt","379ddc42b8202f116d10d8b5bb41b7ee");
        request.addCookie("uid_tt","7002c4204834d0276e6111a0da9ff6d5");
        request.addCookie("UM_distinctid","162bc6147d93b4-079e0320e340c7-1773185e-3d10d-162bc6147da799");
        request.addCookie("_ga","GA1.2.1730464358.1484787036");
        request.addCookie("login_flag","fed9b272cc7d41b477bdd8cab6302d1b");
        request.addCookie("odin_tt","06f0bfedccd961912728241af6c13671c03d88968321516ae002bc5def784930d83a10910a904e601d9377cfb9a3a8c1");
        request.addCookie("uuid","w:d6e0b3c35d9544fea83aebb5d6f3bd39");
        return request;
    }

    @Override
    public Spider createCrawler() {
        Request request = new Request();
        Request resultReq = setCookies(request);
        resultReq.setUrl(LIST_URL);

        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(resultReq);
        return spider;
    }

    @Override
    public synchronized void process(Page page) {
        if(LIST_URL.equals(page.getUrl().toString())){
            // 获取图片列表信息
            this.getPictureList(page);
        }else{
            // 获取图片详情信息
            this.getPictureDetail(page);
        }
    }

    /**
     * 获取图片列表
     * @param page
     */
    private void getPictureList(Page page) {
        logger.info("抓取今日头条-图片信息程序开始...");
        try {
            JSONObject jsonObject = JSON.parseObject(page.getRawText());
            JSONArray data = jsonObject.getJSONArray("data");
            List<String> detail_url_list = new ArrayList<>();
            // 遍历列表图片
            for (int i = 0 ; i < data.size(); i++) {
                JSONObject obj = data.getJSONObject(i);
                JSONObject content = obj.getJSONObject("content");
                String date = sdf.format(new Date());
                Long newId = IDUtil.getNewID();
                Long item_id = content.getLong("item_id");
                if (StringUtils.isEmpty(item_id)) {
                    continue;
                }
                String label = (content.getString("label") != null ? content.getString("label") : "");
                if (label.equals("广告")) {
                    continue;
                }
                String title = content.getString("title");
                Integer hot = (content.getInteger("hot") != null ? content.getInteger("hot") : 0);
                String source = content.getString("source");
                String display_url = content.getString("display_url");
                String article_url = content.getString("article_url");
                Integer comment_count = (content.getInteger("comment_count") != null ? content.getInteger("comment_count") : 0);
                String keywords = content.getString("keywords");
                Integer ban_comment = (content.getInteger("ban_comment") != null ? content.getInteger("ban_comment") : 0);
                String sub_abstract = content.getString("abstract");
                String share_url = content.getString("share_url");
//                Integer share_count = (content.getInteger("share_count") != null ? content.getInteger("share_count") : 0);
                Boolean has_image = (content.getBoolean("has_image") != null ? content.getBoolean("has_image") : false);
                // 列表图片
                JSONArray imageList = content.getJSONArray("image_list");
                if (null != imageList && imageList.size() > 0 ) {
                    JSONObject imgObj = imageList.getJSONObject(0);
                    NewsImageDO imageDO = new NewsImageDO(IDUtil.getNewID(), newId, imgObj.getInteger("width"), imgObj.getInteger("height"),
                            NewsImageDO.IMAGE_TYPE_LARGE, imgObj.getString("url"), NewsImageDO.STATUS_NORMAL);
                    image_list.add(imageDO);
                }
                // 获取列表信息
                NewsDO newsDO =  newsCoreManager.buildNewsDO(newId, item_id.toString(), title, hot, display_url, share_url, source, article_url,
                        NewsDO.NEWS_TYPE_PICTURE, NewsDO.CONTENT_TYPE_LARGE_PIC, keywords, ban_comment,
                        sub_abstract, sdf.parse(date), 0, (has_image ? 1 : 0), sdf.parse(date),
                        NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE, comment_count, comment_count);
                // 获取item_id 和 group_id 用于拼接详情url
                list.add(newsDO);
                // 图片详情请求url
                String IMG_URL = "http://a3.pstatp.com/article/content/19/2/";
                StringBuilder sb = new StringBuilder(IMG_URL);
                sb.append(item_id.toString()).append("/").append(item_id.toString()).append("/1/0/");
                detail_url_list.add(sb.toString());
            }
            // 新增新闻列表
            newsMapper.insertNews(list);
            // 新增新闻列表图片
            newsImageMapper.insertNewsImage(image_list);
//            JSONArray ja = JSONArray.parseArray(JSON.toJSONString(list));
            page.addTargetRequests(detail_url_list);
        } catch (Exception e) {
            logger.error("抓取今日头条-图片信息失败：" + e);
            e.printStackTrace();
        }
    }

    /**
     * 获取图片详情
     * @param page
     */
    private void getPictureDetail(Page page){
       try {
           JSONObject jsonObject = JSON.parseObject(page.getRawText());
           JSONObject data = jsonObject.getJSONObject("data");
           String item_id = data.getLong("item_id").toString();
           JSONArray array = data.getJSONArray("gallery");
           // 处理图文详情
           for (int i = 0 ; i < array.size();i++) {
               JSONObject imageObj = array.getJSONObject(i);
               JSONObject img = imageObj.getJSONObject("sub_image");
               String sub_abstract = imageObj.getString("sub_abstract");
               // 上传图片至阿里云
               String url = aliyunOSSClientUtil.uploadPicture(img.getString("url"));
               NewsContentPictureDO pictureDO = new NewsContentPictureDO(IDUtil.getNewID(), 0L, (i+1), url, img.getInteger("width"),
                       img.getInteger("height"), sub_abstract, NewsContentPictureDO.STATUS_NORMAL);
               for (NewsDO news : list) {
                   if (item_id.equals(news.getDataKey())) {
                       pictureDO.setNewsId(news.getNewsId());
                       break;
                   }
               }
               // 新增大图新闻
               newsContentPictureMapper.insertPicture(pictureDO);
           }
//           JSONArray ja = JSONArray.parseArray(JSON.toJSONString(picture_list));
       } catch (Exception e) {
           logger.error("抓取今日头条-图片详情信息失败：" + e);
           e.printStackTrace();
       }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
