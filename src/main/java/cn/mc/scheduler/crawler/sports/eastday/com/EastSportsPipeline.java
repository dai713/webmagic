package cn.mc.scheduler.crawler.sports.eastday.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.scheduler.crawler.CrawlerManager;
import cn.mc.scheduler.mapper.NewsContentArticleMapper;
import cn.mc.scheduler.mapper.NewsImageMapper;
import cn.mc.scheduler.mapper.NewsMapper;
import cn.mc.scheduler.mq.MQTemplate;
import cn.mc.scheduler.util.CrawlerUtil;
import cn.mc.scheduler.util.SchedulerUtils;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 东方体育新闻 - 保存
 *
 * @author daiqingwen
 * @date 2018-7-30 下午17:52
 */

@Component
public class EastSportsPipeline {
    @Autowired
    private NewsContentArticleMapper newsContentArticleMapper;
    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsImageMapper newsImageMapper;
    @Autowired
    private CrawlerManager crawlerManager;
    @Autowired
    private MQTemplate mqTemplate;
    @Autowired
    private CrawlerUtil crawlerUtil;
    /**
     * 保存
     * @param dataKey
     * @param newsMap
     * @param imageMap
     * @param contentArticleDO
     */
    @Transactional
    public synchronized void save(String dataKey, Map<String,NewsDO> newsMap, Map<String,List<NewsImageDO>> imageMap, NewsContentArticleDO contentArticleDO) {
        NewsDO newsDO = newsMap.get(dataKey);

        List<NewsImageDO> newsImageDO = imageMap.get(dataKey);

        // 过滤内容
        String content = SchedulerUtils.contentFilter(contentArticleDO.getArticle(), newsDO.getNewsSource(),newsDO.getTitle(), newsDO.getNewsId());

        if (StringUtils.isEmpty(content)) {
            return;
        }

        content = content.replaceAll("\\n", "");

        // 检查数据库是否存在 新闻
        NewsDO dataBaseNewsDO = crawlerManager.getNewsDOByDataKey(newsDO.getDataKey(), new Field("newsId"));

        if (!StringUtils.isEmpty(dataBaseNewsDO)) {
            return;
        }

        contentArticleDO.setNewsId(newsDO.getNewsId());
        contentArticleDO.setArticle(content);

        // 保存数据
        newsMapper.insert(Update.copyWithoutNull(newsDO));
        //添加新闻缓存时间 用来监控
        crawlerUtil.addNewsTime(this.getClass().getSimpleName()+newsDO.getNewsType());

        newsContentArticleMapper.insert(Update.copyWithoutNull(contentArticleDO));

        for (NewsImageDO imageDO : newsImageDO) {
            newsImageMapper.insert(Update.copyWithoutNull(imageDO));
        }

        // 发送mq
        mqTemplate.sendNewsReviewMessage(MQTemplate.ARTICLE_TAG, ImmutableMap.of("newsId", newsDO.getNewsId()));

    }

}
