package cn.mc.scheduler.crawler.engadget.com;

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
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import cn.mc.scheduler.util.CrawlerUtil;
import cn.mc.scheduler.util.SchedulerUtils;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * engadget新闻 - 保存
 *
 * @author daiqingwen
 * @date 2018-7-25 上午 11:13
 */
@Component
public class EngadgetPipeline {

    @Autowired
    private NewsContentArticleMapper newsContentArticleMapper;
    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsImageMapper newsImageMapper;
    @Autowired
    private MQTemplate mqTemplate;
    @Autowired
    private CrawlerManager crawlerManager;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private CrawlerUtil crawlerUtil;

    /**
     * 保存新闻
     * @param dataKey
     * @param map
     * @param articleDO
     * @param imageMap
     */
    @Transactional
    public synchronized void toSave(String dataKey,
                                    Map<String, NewsDO> map,
                                    Map<String, NewsImageDO> imageMap,
                                    NewsContentArticleDO articleDO) {

        if (!map.containsKey(dataKey) || !imageMap.containsKey(dataKey)) {
            return;
        }
        // 检查数据库是否存在 新闻
        NewsDO dataBaseNewsDO = crawlerManager.getNewsDOByDataKey(
                dataKey, new Field("newsId"));

        if (!StringUtils.isEmpty(dataBaseNewsDO)) {
            return;
        }
        NewsDO newsDO = map.get(dataKey);
        NewsImageDO newsImageDO = imageMap.get(dataKey);

        // 过滤内容
        String content = SchedulerUtils.contentFilter(
                articleDO.getArticle(), newsDO.getNewsSource(), newsDO.getTitle(),newsDO.getNewsId());

        if (StringUtils.isEmpty(content)) {
            return;
        }

        // content 里面的图片上传到 aliYun 并替换地址
        content=aliyunOSSClientUtil.replaceSourcePicToOSS(content);

        articleDO.setNewsId(newsDO.getNewsId());
        articleDO.setArticle(content);

        // 上传图片至阿里云
        String ossImageUrl = aliyunOSSClientUtil.uploadPicture(newsImageDO.getImageUrl());
        newsImageDO.setImageUrl(ossImageUrl);
        newsImageMapper.insert(Update.copyWithoutNull(newsImageDO));

        // 保存数据
        newsMapper.insert(Update.copyWithoutNull(newsDO));
        //添加新闻缓存时间 用来监控
        crawlerUtil.addNewsTime(this.getClass().getSimpleName()+newsDO.getNewsType());

        newsContentArticleMapper.insert(Update.copyWithoutNull(articleDO));

        // 发送mq
        mqTemplate.sendNewsReviewMessage(MQTemplate.ARTICLE_TAG,
                ImmutableMap.of("newsId", newsDO.getNewsId()));
    }
}
