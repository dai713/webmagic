package cn.mc.scheduler.crawler.wukong.com;

import cn.mc.core.dataObject.*;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.CollectionUtil;
import cn.mc.scheduler.crawler.CrawlerManager;
import cn.mc.scheduler.crawler.CrawlerSupport;
import cn.mc.scheduler.mapper.*;
import cn.mc.scheduler.mq.MQTemplate;
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @auther sin
 * @time 2018/5/8 16:40
 */
@Service
public class HotCrawlerPipeline {

    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsContentAnswerMapper newsContentAnswerMapper;
    @Autowired
    private NewsQuestionAnswerUserMapper newsQuestionAnswerUserMapper;
    @Autowired
    private NewsQuestionAnswerImageMapper newsQuestionAnswerImageMapper;
    @Autowired
    private NewsContentQuestionMapper newsContentQuestionMapper;
    @Autowired
    private NewsContentQuestionImageMapper newsContentQuestionImageMapper;
    @Resource
    private AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private MQTemplate mqTemplate;
    @Autowired
    private CrawlerManager crawlerManager;

    public void save(HotCrawlerVO hotCrawlerBO) {
        doSave(hotCrawlerBO);
    }

    @Transactional
    public synchronized void doSave(HotCrawlerVO hotCrawlerBO) {
        if (hotCrawlerBO == null) {
            return;
        }

        List<NewsContentAnswerDO> newsContentAnswerDOList
                = hotCrawlerBO.getNewsContentAnswerDOList();
        List<NewsQuestionAnswerUserDO> newsQuestionAnswerUserDOList
                = hotCrawlerBO.getNewsQuestionAnswerUserDOList();
        List<NewsQuestionAnswerImageDO> newsQuestionAnswerImageDOList
                = hotCrawlerBO.getNewsQuestionAnswerImageDOList();

        // 没有回答直接过滤
        if (CollectionUtils.isEmpty(newsContentAnswerDOList)
                || null == hotCrawlerBO.getNewsDO()) {
            return;
        }

        NewsDO newsDO = hotCrawlerBO.getNewsDO();

        // 过滤好的内容 优先级 video、image、text
        NewsContentAnswerDO finalNewsContentAnswerDO;
        NewsContentAnswerDO imageNewsContentAnswerDO = null;
        NewsContentAnswerDO videoNewsContentAnswerDO = null;
        NewsContentAnswerDO firstNewsContentAnswerDO = null;
        int newsContentAnswerDOIndex = 0;

        //评论总数
        int newsTotalSourceCommentCount = 0;
        for (NewsContentAnswerDO newsContentAnswerDO : newsContentAnswerDOList) {
            if (newsContentAnswerDOIndex == 0) {
                firstNewsContentAnswerDO = newsContentAnswerDO;
            }
            if (newsContentAnswerDO.getImageCount() > 0) {
                imageNewsContentAnswerDO = newsContentAnswerDO;
            }
            if (newsContentAnswerDO.getVideoCount() > 0) {
                videoNewsContentAnswerDO = newsContentAnswerDO;
            }

            // 累加总评论数
            newsTotalSourceCommentCount += newsContentAnswerDO.getSourceCommentCount();
            newsContentAnswerDOIndex ++;
        }

        // 设置源评论数
        newsDO.setSourceCommentCount(newsTotalSourceCommentCount);
        // 设置评论数
        newsDO.setCommentCount(0);

        if (videoNewsContentAnswerDO != null) {
            finalNewsContentAnswerDO = videoNewsContentAnswerDO;
        } else if (imageNewsContentAnswerDO != null) {
            finalNewsContentAnswerDO = imageNewsContentAnswerDO;
        } else if (firstNewsContentAnswerDO == null) {
            finalNewsContentAnswerDO = firstNewsContentAnswerDO;
        } else {
            return;
        }

        // 显示类型
        Integer displayType;
        if (finalNewsContentAnswerDO.getVideoCount() != 0) {
            displayType = CrawlerSupport.handleVideoDisplayType();
        } else {
            displayType = CrawlerSupport.handleNewsDisplayType(finalNewsContentAnswerDO.getImageCount());
        }
        newsDO.setDisplayType(displayType);

        // 过滤号的内容 设置到newsDO
        newsDO.setVideoCount(finalNewsContentAnswerDO.getVideoCount());
        newsDO.setImageCount(finalNewsContentAnswerDO.getImageCount());
        newsDO.setNewsAbstract(finalNewsContentAnswerDO.getContentAbstract());
        newsDO.setRecommendAnswerId(finalNewsContentAnswerDO.getNewsContentAnswerId());

        // 回答数量
        newsDO.setQaCount(newsContentAnswerDOList.size());
        NewsDO dataBaseNewsDO = crawlerManager.listNewsDOByDataKey(
                newsDO.getDataKey(), new Field("newsId"));

        // TODO: 2018/4/2 先这样 有就不插入了
        if (dataBaseNewsDO != null) {
            return;
        }
        // 保存 news
        // TODO: 2018/4/2 先这样
        if (dataBaseNewsDO == null) {
            newsDO.setNewsState(NewsDO.STATE_NOT_RELEASE);
            newsMapper.insert(Update.copyWithoutNull(newsDO));

            // 问题 - 内容
            NewsContentQuestionDO newsContentQuestionDO
                    = hotCrawlerBO.getNewsContentQuestionDO();
            newsContentQuestionMapper.insert(
                    Update.copyWithoutNull(newsContentQuestionDO));

            // 问题 - 图片
            List<NewsContentQuestionImageDO> newsContentQuestionImageDOList
                    = hotCrawlerBO.getNewsContentQuestionImageDOList();

            for (NewsContentQuestionImageDO newsContentQuestionImageDO
                    : newsContentQuestionImageDOList) {
                //上传阿里云替换成我们图片地址
                newsContentQuestionImageDO.setImageUrl(aliyunOSSClientUtil.replaceSourcePicToOSS(newsContentQuestionImageDO.getImageUrl()));
                newsContentQuestionImageMapper.insert(
                        Update.copyWithoutNull(newsContentQuestionImageDO));
            }
        }

        //
        // 如果问答已存在
        // 添加新的 回答

        // TODO: 2018/4/2 需要优化 爬虫 ，结构需要重新构建
        // 保存 回答
        Set<String> contentAnswerDataKeys = CollectionUtil.buildSet(
                newsContentAnswerDOList, String.class, "dataKey");

        Map<String, NewsContentAnswerDO> newsContentAnswerDOMap = CollectionUtil.buildMap(
                newsContentAnswerMapper.selectByDataKeys(contentAnswerDataKeys,
                        new Field("dataKey")), String.class,
                NewsContentAnswerDO.class, "dataKey");

        // 插入的 回答 ids，给下面进行使用
        for (NewsContentAnswerDO newsContentAnswerDO : newsContentAnswerDOList) {
            if (!newsContentAnswerDOMap.containsKey(newsContentAnswerDO.getDataKey())) {

                // 推荐的内容，hot = 1
                if (finalNewsContentAnswerDO.getNewsContentAnswerId()
                        .equals(newsContentAnswerDO.getNewsContentAnswerId())) {
                    newsContentAnswerDO.setHot(1);
                }
                //上传阿里云替换成我们图片地址
                newsContentAnswerDO.setContent(aliyunOSSClientUtil.replaceSourcePicToOSS(newsContentAnswerDO.getContent()));
                newsContentAnswerMapper.insert(Update.copyWithoutNull(newsContentAnswerDO));
            }
        }

        // 回答 - 用户
        if (!CollectionUtils.isEmpty(newsQuestionAnswerUserDOList)) {
            Set<String> dataKeys = CollectionUtil.buildSet(
                    newsQuestionAnswerUserDOList, String.class, "dataKey");

            List<NewsQuestionAnswerUserDO> baseNewsQuestionAnswerUserList
                    = CollectionUtil.removeNullData(newsQuestionAnswerUserMapper.selectByDataKeys(dataKeys, new Field("dataKey")));
            Map<String, NewsQuestionAnswerUserDO> newsQuestionAnswerUserDOMap = CollectionUtil.buildMap(
                    baseNewsQuestionAnswerUserList,
                    String.class, NewsQuestionAnswerUserDO.class, "dataKey");

            for (NewsQuestionAnswerUserDO newsQuestionAnswerUserDO : newsQuestionAnswerUserDOList) {
                if (!newsQuestionAnswerUserDOMap.containsKey(newsQuestionAnswerUserDO.getDataKey())) {
                    //上传阿里云替换成我们图片地址
                    newsQuestionAnswerUserDO.setAvatarUrl(aliyunOSSClientUtil.replaceSourcePicToOSS(newsQuestionAnswerUserDO.getAvatarUrl()));
                    newsQuestionAnswerUserMapper.insert(Update.copyWithoutNull(newsQuestionAnswerUserDO));
                }
            }
        }

        // 回答 - 图片
        if (!CollectionUtils.isEmpty(newsQuestionAnswerImageDOList)) {
            Set<String> dataKeys = CollectionUtil.buildSet(
                    newsQuestionAnswerImageDOList, String.class, "dataKey");
            Map<String, NewsQuestionAnswerImageDO> newsQuestionAnswerImageDOMap = CollectionUtil.buildMap(
                    newsQuestionAnswerImageMapper.selectByDataKeys(dataKeys,
                            new Field("dataKey")), String.class,
                    NewsQuestionAnswerImageDO.class, "dataKey");
            for (NewsQuestionAnswerImageDO newsQuestionAnswerImageDO : newsQuestionAnswerImageDOList) {
                if (!newsQuestionAnswerImageDOMap.containsKey(newsQuestionAnswerImageDO.getDataKey())) {
                    //上传阿里云替换成我们图片地址
                    newsQuestionAnswerImageDO.setImageUrl(aliyunOSSClientUtil.replaceSourcePicToOSS(newsQuestionAnswerImageDO.getImageUrl()));
                    newsQuestionAnswerImageMapper.insert(Update.copyWithoutNull(newsQuestionAnswerImageDO));
                }
            }
        }

        // 发送mq
        mqTemplate.sendNewsReviewMessage(MQTemplate.ARTICLE_TAG,
                ImmutableMap.of("newsId", newsDO.getNewsId()));
    }
}
