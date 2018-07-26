package cn.mc.scheduler.crawler.geekpark.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsCoreManager;
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
 * 极客公园新闻
 *
 * @author daiqingwen
 * @date 2018/7/23 下午 15:46
 */

@Component
public class GeekParkCrawlers extends BaseCrawler {

    private static final Logger log = LoggerFactory.getLogger(GeekParkCrawlers.class);

    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(1000);

    private static final String url = "http://main_test.geekpark.net/api/v1/columns/81?page=1";

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private SchedulerUtils schedulerUtils;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private GeekParkPipeline geekParkPipeline;

    private List<NewsDO> list = Lists.newArrayList();
    private List<NewsContentArticleDO> detailList = Lists.newArrayList();
    private List<NewsImageDO> imageList = Lists.newArrayList();

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
        log.info("抓取极客公园新闻...");
        try {
            JSONObject jsonObject = JSON.parseObject(page.getRawText());
            JSONObject data = jsonObject.getJSONObject("column");
            JSONArray array = data.getJSONArray("posts");

            // 获取第一条新闻
            String firstId = String.valueOf(array.getJSONObject(0).getInteger("id"));
            String firstImg = array.getJSONObject(0).getString("cover_url");
            String cache = (String) redisUtil.get(firstId);
            if(!StringUtils.isEmpty(cache)) {
                if (firstImg.equals(cache)) {
                    return;
                } else {
                    redisUtil.setString(firstId, firstImg);
                }
            } else {
                redisUtil.setString(firstId, firstImg);
            }

            List<String> urlList = Lists.newArrayList();

            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = String.valueOf(obj.getInteger("id"));
                String imgUrl = obj.getString("cover_url");
                Long newId = IDUtil.getNewID();
                String dataKey = this.encrypt(String.valueOf(IDUtil.getNewID()));
                JSONArray img = obj.getJSONArray("img_list");
                Date date = new Date();
                String detailsUrl = "http://main_test.geekpark.net/api/v1/posts/";
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
                // 拼接详情地址
                detailsUrl = detailsUrl + id;

                if (!StringUtils.isEmpty(id)) {
                    urlList.add(detailsUrl);
                }
                NewsDO newsDO = newsCoreManager.buildNewsDO(newId, dataKey, obj.getString("title"), 0, detailsUrl, "", NewsDO.DATA_SOURCE_GEEKPARK, detailsUrl,
                        NewsDO.NEWS_TYPE_TECHNOLOGY, NewsDO.CONTENT_TYPE_IMAGE_TEXT, "", 0, obj.getString("abstract"), date,
                        0, img.size(), date, NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE, obj.getInteger("comments_count"), obj.getInteger("comments_count"));
                list.add(newsDO);

                // 上传图片至阿里云
                String saveImg = aliyunOSSClientUtil.uploadPicture(imgUrl);

                NewsImageDO newsImage = new NewsImageDO(IDUtil.getNewID(), newId, srcWidth, srcHeight, NewsImageDO.IMAGE_TYPE_MINI, saveImg, NewsImageDO.STATUS_NORMAL);
                imageList.add(newsImage);

            }

            page.addTargetRequests(urlList);
        } catch (Exception e) {
            log.error("抓取极客新闻失败：{}", e);
        }

    }

    /**
     * 获取新闻详情
     * @param page
     */
    private void getDetails(Page page) {
        JSONObject jsonObject = JSON.parseObject(page.getRawText());
        JSONObject data = jsonObject.getJSONObject("post");
        String id;
        String detailsId = String.valueOf(data.getInteger("id"));
        Long newsId = 0L;
        for (NewsDO news : list) {
            String newsUrl = news.getNewsUrl();
            id = newsUrl.substring(newsUrl.lastIndexOf("/")+1, newsUrl.length());
            if (detailsId.equals(id)) {
                newsId = news.getNewsId();
                break;
            }
        }
        String content = schedulerUtils.replaceLabel(data.getString("content"));
        NewsContentArticleDO contentArticleDO = new NewsContentArticleDO(IDUtil.getNewID(), newsId, content, NewsContentArticleDO.ARTICLE_TYPE_HTML);
        detailList.add(contentArticleDO);

        if (list.size() == detailList.size()) {
            geekParkPipeline.save(list, detailList, imageList);
        }
    }


}
