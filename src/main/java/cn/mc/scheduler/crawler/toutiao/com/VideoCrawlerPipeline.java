package cn.mc.scheduler.crawler.toutiao.com;

import cn.mc.core.dataObject.NewsContentVideoDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.mybatis.Update;
import cn.mc.scheduler.mapper.NewsContentVideoMapper;
import cn.mc.scheduler.mapper.NewsImageMapper;
import cn.mc.scheduler.mapper.NewsMapper;
import cn.mc.scheduler.mq.MQTemplate;
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import cn.mc.scheduler.util.CrawlerUtil;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @auther sin
 * @time 2018/3/15 17:30
 */
@Service
public class VideoCrawlerPipeline {

    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsImageMapper newsImageMapper;
    @Autowired
    private NewsContentVideoMapper newsContentVideoMapper;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private MQTemplate mqTemplate;
    @Autowired
    private CrawlerUtil crawlerUtil;
    @Transactional
    public synchronized void saveVideo(NewsDO newsDO, NewsContentVideoDO newsContentVideoDO, VideoCrawlerVO videoCrawlerVO) {
        // 保存信息
        newsDO.setNewsState(NewsDO.STATE_NOT_RELEASE);
        newsMapper.insert(Update.copyWithoutNull(newsDO));
        //添加新闻缓存时间 用来监控
        crawlerUtil.addNewsTime(this.getClass().getSimpleName()+newsDO.getNewsType());

        //上传阿里云替换成我们图片地址
        newsContentVideoDO.setVideoImage(aliyunOSSClientUtil.replaceSourcePicToOSS(newsContentVideoDO.getVideoImage()));

        newsContentVideoMapper.insert(Update.copyWithoutNull(newsContentVideoDO));


        List<NewsImageDO> newsImageDOList = videoCrawlerVO.getNewsImageDOList();
        for (NewsImageDO newsImageDO : newsImageDOList) {
            //上传阿里云替换成我们图片地址
            newsImageDO.setImageUrl(aliyunOSSClientUtil.replaceSourcePicToOSS(newsImageDO.getImageUrl()));
            newsImageMapper.insert(Update.copyWithoutNull(newsImageDO));
        }

        // 发送mq
        mqTemplate.sendNewsReviewMessage(MQTemplate.VIDEO_TAG,
                ImmutableMap.of("newsId", newsDO.getNewsId()));
    }
}
