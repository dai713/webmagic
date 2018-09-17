package cn.mc.scheduler.crawlerOverseas;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsContentOverseasArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsContentArticleCoreManager;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.IDUtil;
import cn.mc.core.utils.translation.TranslationUtil;
import cn.mc.scheduler.crawler.CrawlerManager;
import cn.mc.scheduler.mapper.NewsContentArticleMapper;
import cn.mc.scheduler.mapper.NewsContentOverseasArticleMapper;
import cn.mc.scheduler.mapper.NewsImageMapper;
import cn.mc.scheduler.mapper.NewsMapper;
import cn.mc.scheduler.mq.MQTemplate;
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import cn.mc.scheduler.util.CrawlerUtil;
import cn.mc.scheduler.util.SchedulerUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 海外新闻保存
 *
 * @author Sin
 * @time 2018/8/28 下午8:12
 */
@Component
public class OverseasCrawlerPipeline {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverseasCrawlerPipeline.class);

    @Autowired
    private MQTemplate mqTemplate;
    @Autowired
    private CrawlerManager crawlerManager;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private CrawlerUtil crawlerUtil;
    @Autowired
    private NewsContentArticleCoreManager newsContentArticleCoreManager;

    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsImageMapper newsImageMapper;
    @Autowired
    private NewsContentArticleMapper newsContentArticleMapper;
    @Autowired
    private NewsContentOverseasArticleMapper newsContentOverseasArticleMapper;

    public void save(
            @NotNull NewsDO newsDO,
            @NotNull List<NewsImageDO> newsImageDOList,
            @NotNull NewsContentOverseasArticleDO newsContentOverseasArticleDO) {

        // 检查 dataKey 是否重复，之确保前 ? 个月不重复
        String dataKey = newsDO.getDataKey();
        NewsDO baseNewsDO = crawlerManager.getNewsDOByDataKey(dataKey, new Field());

        if (baseNewsDO != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("dataKey 已存在！自动过滤！newsDO {}", newsDO);
            }
            return;
        }

        // 获取自己字段
        Long newsId = newsDO.getNewsId();
        String title = newsDO.getTitle();
        String newsSource = newsDO.getNewsSource();

        // 有限上传图片，避免中英文，内容图片不一致
        String articleHtml = newsContentOverseasArticleDO.getArticle();
        articleHtml = SchedulerUtils.filterHtmlLabel(articleHtml);
        articleHtml = aliyunOSSClientUtil.replaceSourcePicToOSS(articleHtml);

        // 翻译内容
        String newsTitle = newsDO.getTitle();
        String newsAbstract = newsDO.getNewsAbstract();
        String translationHtml = TranslationUtil.translationWithHtml(articleHtml);
        String translationTitle = TranslationUtil.translation(newsTitle);
        String translationNewsAbstract = TranslationUtil.translation(newsAbstract);

        // 设置 title
        newsDO.setTitle(translationTitle);
        newsDO.setNewsAbstract(translationNewsAbstract);

        // 转换 newsContentArticle
        Long newsContentArticleId = IDUtil.getNewID();
        NewsContentArticleDO newsContentArticleDO
                = newsContentArticleCoreManager.buildNewsContentArticleDO(
                newsContentArticleId, newsId, translationTitle, translationHtml);

        // 过滤内容
        String newArticle = SchedulerUtils.contentFilter(translationHtml, newsSource, title, newsId);
        if (newArticle == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("contentFilter 不通过！自动过滤！newsDO {} " +
                        "newsContentArticleDO {}", newsDO, newsContentArticleDO);
            }
            return;
        }

        // 重新设置 文章内容
        newsContentArticleDO.setArticle(newArticle);
        newsContentOverseasArticleDO.setArticle(articleHtml);

        // 上传图片
        for (NewsImageDO newsImageDO : newsImageDOList) {

            // 文件上传 aliYun
            String ossImageUrl = aliyunOSSClientUtil.replaceSourcePicToOSS(newsImageDO.getImageUrl());
            newsImageDO.setImageUrl(ossImageUrl);
        }

        // TODO: 2018/9/14 上线时需要处理
        newsDO.setNewsState(NewsDO.STATE_RELEASE);
        toSave(newsDO, newsImageDOList, newsContentOverseasArticleDO, newsContentArticleDO);
    }

    @Transactional
    void toSave(@NotNull NewsDO newsDO,
                        @NotNull List<NewsImageDO> newsImageDOList,
                        @NotNull NewsContentOverseasArticleDO newsContentOverseasArticleDO,
                        @NotNull NewsContentArticleDO newsContentArticleDO) {
        newsMapper.insert(Update.copyWithoutNull(newsDO));
        newsContentArticleMapper.insert(Update.copyWithoutNull(newsContentArticleDO));
        newsContentOverseasArticleMapper.insert(Update.copyWithoutNull(newsContentOverseasArticleDO));
        for (NewsImageDO newsImageDO : newsImageDOList) {
            newsImageMapper.insert(Update.copyWithoutNull(newsImageDO));
        }

        //添加新闻缓存时间 用来监控
        crawlerUtil.addNewsTime(this.getClass().getSimpleName() + newsDO.getNewsType());

        // 发送mq
//        mqTemplate.sendNewsReviewMessage(MQTemplate.ARTICLE_TAG,
//                ImmutableMap.of("newsId", newsDO.getNewsId()));
    }
}
