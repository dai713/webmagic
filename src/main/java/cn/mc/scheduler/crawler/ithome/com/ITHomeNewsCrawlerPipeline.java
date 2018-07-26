package cn.mc.scheduler.crawler.ithome.com;

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
import cn.mc.scheduler.util.SchedulerUtils;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class ITHomeNewsCrawlerPipeline {

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

    @Transactional
    public synchronized void saveITHomeNews(String dataKey,
                                            Map<String, NewsDO> cacheNewsDO,
                                            Map<String, List<NewsImageDO>> cacheNewsImageDO,
                                            String articleContent,
                                            String source) {

        // 不存在就直接 return false
        if (!cacheNewsDO.containsKey(dataKey)) {
            return;
        }

        // 获取新闻
        NewsDO newsDO = cacheNewsDO.get(dataKey);

        // 过滤内容
        articleContent = SchedulerUtils.contentFilter(articleContent, source,newsDO.getNewsId());

        if (StringUtils.isEmpty(articleContent))
            return;

        // 检查数据库是否存在 新闻
        NewsDO dataBaseNewsDO = crawlerManager.listNewsDOByDataKey(
                newsDO.getDataKey(), new Field("newsId"));

        if (dataBaseNewsDO != null) {
            return;
        }

        // 获取新闻图片
        List<NewsImageDO> newsImageDOList = cacheNewsImageDO.get(dataKey);

        // build 一个文章类型
        NewsContentArticleDO contentArticleDO = newsContentArticleCoreManager
                .buildNewsContentArticleDO(IDUtil.getNewID(), newsDO.getNewsId(), articleContent);
        //上传阿里云替换成我们图片地址
        contentArticleDO.setArticle(aliyunOSSClientUtil.replaceSourcePicToOSS(contentArticleDO.getArticle()));
        newsContentArticleMapper.insert(Update.copyWithoutNull(contentArticleDO));
        newsDO.setNewsSource(source);
        newsDO.setNewsState(NewsDO.STATE_NOT_RELEASE);
        newsMapper.insert(Update.copyWithoutNull(newsDO));

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
