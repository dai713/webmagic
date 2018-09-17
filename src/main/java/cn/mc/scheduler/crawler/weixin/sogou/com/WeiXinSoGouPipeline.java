package cn.mc.scheduler.crawler.weixin.sogou.com;

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
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 搜狗-微信新闻 - 保存
 *
 * @author daiqingwen
 * @date 2018-7-23 下午19:09
 */
@Component
public class WeiXinSoGouPipeline {

    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsImageMapper newsImageMapper;
    @Autowired
    private NewsContentArticleMapper newsContentArticleMapper;
    @Autowired
    private CrawlerManager crawlerManager;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private MQTemplate mqTemplate;
    @Autowired
    private CrawlerUtil crawlerUtil;
    /**
     * 保存新闻
     *
     * @param newsDO
     * @param newsContentArticleDO
     * @param imageList
     */
    @Transactional
    public synchronized void save(@NotNull NewsDO newsDO,
                                  @NotNull NewsContentArticleDO newsContentArticleDO,
                                  @NotNull List<NewsImageDO> imageList) {

        // 检查数据库是否存在该新闻
        String dateKey = newsDO.getDataKey();
        NewsDO dataBaseNewsDO = crawlerManager.getNewsDOByDataKey(
                dateKey, new Field("newsId"));

        if (dataBaseNewsDO != null) {
            return;
        }

        // 过滤内容
        String article = newsContentArticleDO.getArticle();
        article = SchedulerUtils.contentFilter(article,
                newsDO.getNewsSource(),newsDO.getTitle(), newsDO.getNewsId());

        // 处理失败，或者不通过为 null
        if (article == null) {
            return;
        }

        // 将文章的图片上传 oss
        article = aliyunOSSClientUtil.replaceSourcePicToOSS(article);
        newsContentArticleDO.setArticle(article);


        // 去保存
        newsMapper.insert(Update.copyWithoutNull(newsDO));
        //添加新闻缓存时间 用来监控
        crawlerUtil.addNewsTime(this.getClass().getSimpleName()+newsDO.getNewsType());


        newsContentArticleMapper.insert(Update.copyWithoutNull(newsContentArticleDO));

        // 处理图片上传到阿里云，并保存
        for (NewsImageDO newsImageDO : imageList) {
            String uploadImageUrl = aliyunOSSClientUtil
                    .replaceSourcePicToOSS(newsImageDO.getImageUrl());

            newsImageDO.setImageUrl(uploadImageUrl);
            newsImageMapper.insert(Update.copyWithoutNull(newsImageDO));
        }

        // 发送mq
        mqTemplate.sendNewsReviewMessage(MQTemplate.ARTICLE_TAG,
                ImmutableMap.of("newsId", newsDO.getNewsId()));
    }
}
