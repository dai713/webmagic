package cn.mc.scheduler.crawler.santaihu.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsContentArticleCoreManager;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.crawler.CrawlerManager;
import cn.mc.scheduler.mapper.NewsContentArticleMapper;
import cn.mc.scheduler.mapper.NewsImageMapper;
import cn.mc.scheduler.mapper.NewsMapper;
import cn.mc.scheduler.mq.MQTemplate;
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import cn.mc.scheduler.util.CrawlerUtil;
import cn.mc.scheduler.util.SchedulerUtils;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class SantaihuNewsCrawlerPipeline {

    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsImageMapper newsImageMapper;
    @Autowired
    private NewsContentArticleMapper newsContentArticleMapper;
    @Autowired
    private NewsContentArticleCoreManager newsContentArticleCoreManager;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private MQTemplate mqTemplate;
    @Autowired
    private CrawlerManager crawlerManager;
    @Autowired
    private CrawlerUtil crawlerUtil;
    @Transactional
    public synchronized void saveSantaiNews(String dataKey,
                                            NewsDO newsDO,
                                            Map<String, List<NewsImageDO>> cacheNewsImageDO,
                                            String articleContent) {

        // 检查数据库是否存在 新闻
        NewsDO dataBaseNewsDO = crawlerManager.getNewsDOByDataKey(
                newsDO.getDataKey(), new Field("newsId"));

        if (dataBaseNewsDO != null) {
            return;
        }

        // 过滤内容
        articleContent = SchedulerUtils.contentFilter(articleContent, newsDO.getNewsSource(),newsDO.getTitle(),newsDO.getNewsId());

        if (StringUtils.isEmpty(articleContent))
            return;

        // 获取新闻图片
        List<NewsImageDO> newsImageDOList = cacheNewsImageDO.get(dataKey);

        // build 一个文章类型
        NewsContentArticleDO contentArticleDO = newsContentArticleCoreManager
                .buildNewsContentArticleDO(IDUtil.getNewID(), newsDO.getNewsId(), articleContent);
        //上传阿里云替换成我们图片地址
        contentArticleDO.setArticle(aliyunOSSClientUtil.replaceSourcePicToOSS(contentArticleDO.getArticle()));
        newsContentArticleMapper.insert(Update.copyWithoutNull(contentArticleDO));

        newsDO.setNewsState(NewsDO.STATE_NOT_RELEASE);
        newsMapper.insert(Update.copyWithoutNull(newsDO));
        //添加新闻缓存时间 用来监控
        crawlerUtil.addNewsTime(this.getClass().getSimpleName()+newsDO.getNewsType());

        // 新闻 image
        for (NewsImageDO newsImageDO : newsImageDOList) {
            //上传阿里云替换成我们图片地址
            newsImageDO.setImageUrl(aliyunOSSClientUtil.replaceSourcePicToOSS(newsImageDO.getImageUrl()));
            newsImageMapper.insert(Update.copyWithoutNull(newsImageDO));
        }

        // 发送mq
        mqTemplate.sendNewsReviewMessage(MQTemplate.ARTICLE_TAG,
                ImmutableMap.of("newsId", newsDO.getNewsId()));
    }
}
