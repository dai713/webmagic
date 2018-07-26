package cn.mc.scheduler.crawler.ifanr.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.utils.CollectionUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import cn.mc.scheduler.util.RedisUtil;
import cn.mc.scheduler.util.SchedulerUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 爱范儿网新闻
 *
 * @author daiqingwen
 * @date 2018/7/24 下午 17:45
 */

@Component
public class IfanrCrawlers extends BaseCrawler {
    private static final Logger log = LoggerFactory.getLogger(IfanrCrawlers.class);

    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(1000);

    private static final String url = "https://sso.ifanr.com/api/v5/wp/feed/?limit=12&offset=0";

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private SchedulerUtils schedulerUtils;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private IfanrPipeline ifanrPipeline;

    private static List<NewsDO> list = Lists.newArrayList();
    private static List<NewsContentArticleDO> detailList = Lists.newArrayList();
    private static List<NewsImageDO> imageList = Lists.newArrayList();

    @Override
    public Spider createCrawler() {
        Request request = new Request();
        request.setUrl(url);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public void process(Page page) {
        if (url.equals(page.getUrl().toString())) {
            getList(page);
        } else {
            getDetails(page);
        }
    }

    @Override
    public Site getSite() {
        return SITE;
    }

    /**
     * 获取新闻列表
     * @param page
     */
    private void getList(Page page) {
        log.info("开始抓取爱范儿网新闻...");
        try {
            JSONObject jsonObject = JSON.parseObject(page.getRawText());
            JSONArray array = jsonObject.getJSONArray("objects");


            // 获取第一条新闻
            JSONObject firstObject = array.getJSONObject(0);
            firstObject = firstObject.getJSONObject("post");
            String firstId = String.valueOf(firstObject.getInteger("post_id"));
            String firstUrl = firstObject.getString("post_url");
            String cache = (String) redisUtil.get(firstId);
            if(!StringUtils.isEmpty(cache)) {
                if (firstUrl.equals(cache)) {
                    return;
                } else {
                    redisUtil.setString(firstId, firstUrl);
                }
            } else {
                redisUtil.setString(firstId, firstUrl);
            }

            List<String> urlList = Lists.newArrayList();

            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = array.getJSONObject(i).getJSONObject("post");
                Long newId = IDUtil.getNewID();
                String dataKey = this.encrypt(String.valueOf(IDUtil.getNewID()));
                Date date = new Date();
                JSONArray tag = obj.getJSONArray("post_tag");
                String keywords = "";
                if (!CollectionUtil.isEmpty(tag)) {
                    keywords = (String) tag.get(0);
                }
                String title = obj.getString("post_title");
                Integer postId = obj.getInteger("post_id");
                if (StringUtils.isEmpty(postId)) {
                    continue;
                }
                String post_id = String.valueOf(postId);
                String timestamp = obj.getString("created_at");
                String imgUrl = obj.getString("post_cover_image");
                String url = obj.getString("post_url");
                String detailsUrl = "https://www.ifanr.com/api/v3.0/?action=get_content&post_id=";
                String appkey = "&appkey=sg5673g77yk72455af4sd55ea&sign=57bdcafaf9d4ed2fdc6df110f64739f7&timestamp=";
                Integer srcWidth;
                Integer srcHeight;
                Map map = schedulerUtils.parseImg(imgUrl);
                if (StringUtils.isEmpty(map)) {
                    srcHeight = 0;
                    srcWidth = 0;
                } else {
                    srcWidth = (Integer) map.get("width");
                    srcHeight = (Integer) map.get("height");
                }

                detailsUrl = detailsUrl + post_id + appkey + timestamp;
                urlList.add(detailsUrl);

                NewsDO newsDO = newsCoreManager.buildNewsDO(newId, dataKey, title, 0, detailsUrl, "", NewsDO.DATA_SOURCE_IFANR, url,
                        NewsDO.NEWS_TYPE_TECHNOLOGY, NewsDO.CONTENT_TYPE_IMAGE_TEXT, keywords, 0, obj.getString("post_excerpt"), date,
                        0, 1, date, NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE, obj.getInteger("comment_count"), obj.getInteger("comment_count"));
                list.add(newsDO);

                // 上传图片至阿里云
                String saveImg = aliyunOSSClientUtil.uploadPicture(imgUrl);
//                String saveImg = imgUrl;

                NewsImageDO newsImage = new NewsImageDO(IDUtil.getNewID(), newId, srcWidth, srcHeight, NewsImageDO.IMAGE_TYPE_MINI, saveImg, NewsImageDO.STATUS_NORMAL);
                imageList.add(newsImage);

            }

            page.addTargetRequests(urlList);

        } catch (Exception e) {
            log.error("抓取爱范儿网新闻失败：{}", e);
        }

    }

    /**
     * 获取新闻详情
     */
    private void getDetails(Page page) {
        JSONObject jsonObject = JSON.parseObject(page.getRawText());
        JSONObject data = jsonObject.getJSONObject("data");
        String origin = page.getUrl().toString();
        String content = data.getString("content");
        content = schedulerUtils.replaceLabel(content);
        Long newsId = 0L;
        for (NewsDO news : list) {
            String newsUrl = news.getNewsUrl();
            if (origin.equals(newsUrl)) {
                newsId = news.getNewsId();
                break;
            }
        }
        NewsContentArticleDO contentArticleDO = new NewsContentArticleDO(IDUtil.getNewID(), newsId, content, NewsContentArticleDO.ARTICLE_TYPE_HTML);
        detailList.add(contentArticleDO);

        if (list.size() == detailList.size()) {
            ifanrPipeline.save(list, detailList, imageList);
        }

    }

}
