package cn.mc.scheduler.crawler.ifanr.com;


import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.CollectionUtil;
import cn.mc.scheduler.crawler.CrawlerManager;
import cn.mc.scheduler.mapper.NewsContentArticleMapper;
import cn.mc.scheduler.mapper.NewsImageMapper;
import cn.mc.scheduler.mapper.NewsMapper;
import cn.mc.scheduler.mq.MQTemplate;
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import cn.mc.scheduler.util.CrawlerUtil;
import cn.mc.scheduler.util.SchedulerUtils;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 爱范儿网新闻 - 保存
 *
 * @author daiqingwen
 * @date 2018-7-23 下午19:21
 */
@Component
public class IfanrPipeline {

    @Autowired
    private CrawlerManager crawlerManager;
    @Autowired
    private MQTemplate mqTemplate;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private NewsContentArticleMapper newsContentArticleMapper;
    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsImageMapper newsImageMapper;
    @Autowired
    private CrawlerUtil crawlerUtil;
    /**
     * 保存新闻
     *
     * @param newsDOList
     * @param newsImageDOMap
     * @param articleDOMap
     */
    @Transactional
    public synchronized void save(List<NewsDO> newsDOList,
                                  Map<Long, NewsImageDO> newsImageDOMap,
                                  Map<Long, NewsContentArticleDO> articleDOMap) {

        Set<String> dataKeys = CollectionUtil.buildSet(
                newsDOList, String.class, "dataKey");

        List<NewsDO> baseNewsDO = crawlerManager.listNewsDOByDataKeys(
                dataKeys, new Field( "dataKey"));

        Map<String, NewsDO> dataKeyNewsDOMap = CollectionUtil.buildMap(
                baseNewsDO, String.class, NewsDO.class, "dataKey");

        for (NewsDO newsDO : newsDOList) {
            Long newsId = newsDO.getNewsId();

            if (!newsImageDOMap.containsKey(newsId) || !articleDOMap.containsKey(newsId)) {
                continue;
            }

            // 处理重复数据
            String dataKey = newsDO.getDataKey();
            if (dataKeyNewsDOMap.containsKey(dataKey)) {
                continue;
            }

            // 处理 article
            NewsContentArticleDO articleDO = articleDOMap.get(newsId);
            String article = articleDO.getArticle();
            String filterArticle = SchedulerUtils.contentFilter(article, newsDO.getNewsSource(),newsDO.getTitle(), newsId);

            if (StringUtils.isEmpty(filterArticle)) {
                continue;
            }

            // 将内容里面的 image 上传到 oss，替换 image url
            article=aliyunOSSClientUtil.replaceSourcePicToOSS(article);
            articleDO.setArticle(article);
            // 处理 imageDO 上传 阿里云
            NewsImageDO newsImageDO = newsImageDOMap.get(newsId);
            String ossImageUrl = aliyunOSSClientUtil.uploadPicture(newsImageDO.getImageUrl());
            newsImageDO.setImageUrl(ossImageUrl);

            // 保存
            newsMapper.insert(Update.copyWithoutNull(newsDO));
            //添加新闻缓存时间 用来监控
            crawlerUtil.addNewsTime(this.getClass().getSimpleName()+newsDO.getNewsType());
            newsContentArticleMapper.insert(Update.copyWithoutNull(articleDO));
            newsImageMapper.insert(Update.copyWithoutNull(newsImageDO));

            // 发送mq
            mqTemplate.sendNewsReviewMessage(MQTemplate.ARTICLE_TAG, ImmutableMap.of("newsId", newsDO.getNewsId()));
        }
    }
}
