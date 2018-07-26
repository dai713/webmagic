package cn.mc.scheduler.crawler.news.baidu.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsContentPictureDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.CommonUtil;
import cn.mc.scheduler.crawler.CrawlerManager;
import cn.mc.scheduler.mapper.NewsContentArticleMapper;
import cn.mc.scheduler.mapper.NewsContentPictureMapper;
import cn.mc.scheduler.mapper.NewsImageMapper;
import cn.mc.scheduler.mapper.NewsMapper;
import cn.mc.scheduler.mq.MQTemplate;
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import cn.mc.scheduler.util.SchedulerUtils;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 百度搜索 下的新闻爬虫
 *
 * @auther sin
 * @time 2018/3/13 14:29
 */
@Service
public class SearchNewsCrawlerPipeline {

    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private CrawlerManager crawlerManager;
    @Autowired
    private MQTemplate mqTemplate;

    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsImageMapper newsImageMapper;
    @Autowired
    private NewsContentArticleMapper newsContentArticleMapper;
    @Autowired
    private NewsContentPictureMapper newsContentPictureMapper;

    private static int size = 0;

    @Transactional
    public synchronized void saveDetailPage(String dataKey,
                                            NewsDO newsDO,
                                            SearchNewsOneVO searchNewsOneVO,
                                            NewsContentArticleDO newsContentArticleDO) {

        // 过滤检查数据
        String newsSource = newsDO.getNewsSource();
        String article = newsContentArticleDO.getArticle();

        String newContent = SchedulerUtils
                .contentFilter(article, newsSource,newsDO.getNewsId());

        if (StringUtils.isEmpty(newContent)) {
            return;
        }
        newsContentArticleDO.setArticle(newContent);

        // 开始保存
        NewsDO dataBaseNewsDO = crawlerManager.listNewsDOByDataKey(
                newsDO.getDataKey(), new Field("newsId"));
        if (dataBaseNewsDO != null) {
            return;
        }

        newsDO.setNewsState(NewsDO.STATE_NOT_RELEASE);
        newsMapper.insert(Update.copyWithoutNull(newsDO));

        // 插入图片
        List<NewsImageDO> newsImageDOList = searchNewsOneVO.getNewsImageDOList();
        for (NewsImageDO newsImageDO : newsImageDOList) {
            //上传阿里云替换成我们图片地址
            newsImageDO.setImageUrl(aliyunOSSClientUtil.replaceSourcePicToOSS(newsImageDO.getImageUrl()));
            newsImageMapper.insert(Update.copyWithoutNull(newsImageDO));
        }
        //上传阿里云替换成我们图片地址
        newsContentArticleDO.setArticle(aliyunOSSClientUtil.replaceSourcePicToOSS(newsContentArticleDO.getArticle()));
        // 插入文字
        newsContentArticleMapper.insert(Update.copyWithoutNull(newsContentArticleDO));
        // 发送mq
        mqTemplate.sendNewsReviewMessage(MQTemplate.ARTICLE_TAG,
                ImmutableMap.of("newsId", newsDO.getNewsId()));

//        CommonUtil.debug("\r\n \r\n size --------> " + ++size);

    }


    @Transactional
    public synchronized void savePicture(NewsDO newsDO,
                                         SearchNewsOneVO searchNewsOneVO,
                                         List<NewsContentPictureDO> newsContentPictureDOList) {

        NewsDO dataBaseNewsDO = crawlerManager.listNewsDOByDataKey(
                newsDO.getDataKey(), new Field("newsId"));

        if (dataBaseNewsDO != null) {
            return;
        }

        // 保存多图，详细数据
        for (NewsContentPictureDO newsContentPictureDO : newsContentPictureDOList) {
            //上传阿里云替换成我们图片地址
            newsContentPictureDO.setImageUrl(aliyunOSSClientUtil.replaceSourcePicToOSS(newsContentPictureDO.getImageUrl()));
            newsContentPictureMapper.insert(Update.copyWithoutNull(newsContentPictureDO));
        }
        // 保存图片
        List<NewsImageDO> newsImageDOList = searchNewsOneVO.getNewsImageDOList();
        for (NewsImageDO newsImageDO : newsImageDOList) {
            //上传阿里云替换成我们图片地址
            newsImageDO.setImageUrl(aliyunOSSClientUtil.replaceSourcePicToOSS(newsImageDO.getImageUrl()));
            newsImageMapper.insert(Update.copyWithoutNull(newsImageDO));
        }
        // 保存 news
        newsDO.setNewsState(NewsDO.STATE_NOT_RELEASE);
        newsMapper.insert(Update.copyWithoutNull(newsDO));
        // 发送mq
        mqTemplate.sendNewsReviewMessage(MQTemplate.PICTURES_TAG,
                ImmutableMap.of("newsId", newsDO.getNewsId()));

//        CommonUtil.debug("\r\n \r\n size --------> " + ++size);
    }
}
