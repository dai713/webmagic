package cn.mc.scheduler.crawler.news.baidu.com;

import cn.mc.core.dataObject.NewsContentVideoDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.scheduler.crawler.CrawlerManager;
import cn.mc.scheduler.mapper.NewsContentVideoMapper;
import cn.mc.scheduler.mapper.NewsImageMapper;
import cn.mc.scheduler.mapper.NewsMapper;
import cn.mc.scheduler.mq.MQTemplate;
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import cn.mc.scheduler.util.CrawlerUtil;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 百度搜索 下的新闻爬虫
 *
 * @auther sin
 * @time 2018/3/13 14:29
 */
@Component
public class BaiDuSportsVideoPipeline {

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
    private NewsContentVideoMapper newsContentVideoMapper;
    @Autowired
    private CrawlerUtil crawlerUtil;
    @Transactional
    public synchronized void save(String dataKey,
                                  Map<String, NewsDO> cacheNewsDO,
                                  Map<String, NewsContentVideoDO> cacheContentVideoDO,
                                  Map<String, List<NewsImageDO>> cacheNewsImageDO) {

        // dataKey 数据不存在直接 return
        if (!cacheNewsDO.containsKey(dataKey)
                || !cacheContentVideoDO.containsKey(dataKey)) {
            return;
        }

        NewsDO newsDO = cacheNewsDO.get(dataKey);

        // 检查重复新闻
        NewsDO dataBaseNewsDO = crawlerManager.getNewsDOByDataKey(
                newsDO.getDataKey(), new Field("newsId"));

        if (dataBaseNewsDO != null) {
            return;
        }

        ///
        /// 图片上传到 阿里云，然后替换视频的分明图片

        List<NewsImageDO> newsImageDOList = cacheNewsImageDO.get(dataKey);

        for (NewsImageDO newsImageDO : newsImageDOList) {
            newsImageDO.setImageUrl(aliyunOSSClientUtil.replaceSourcePicToOSS(newsImageDO.getImageUrl()));
        }

        NewsContentVideoDO newsContentVideoDO = cacheContentVideoDO.get(dataKey);
        NewsImageDO newsImageDOOne = newsImageDOList.get(0);
        newsContentVideoDO.setVideoImage(newsImageDOOne.getImageUrl());

        ///
        /// 开始保存数据库

        // 保存新闻
        newsDO.setNewsState(NewsDO.STATE_NOT_RELEASE);
        newsMapper.insert(Update.copyWithoutNull(newsDO));
        //添加新闻缓存时间 用来监控
        crawlerUtil.addNewsTime(this.getClass().getSimpleName()+newsDO.getNewsType());

        // 保存视频
        newsContentVideoMapper.insert(Update.copyWithoutNull(newsContentVideoDO));

        // 保存图片
        for (NewsImageDO newsImageDO : newsImageDOList) {
            newsImageDO.setImageUrl(aliyunOSSClientUtil.replaceSourcePicToOSS(newsImageDO.getImageUrl()));
            newsImageMapper.insert(Update.copyWithoutNull(newsImageDO));
        }

        // 发送mq
        mqTemplate.sendNewsReviewMessage(MQTemplate.VIDEO_TAG,
                ImmutableMap.of("newsId", newsDO.getNewsId()));
    }
}
