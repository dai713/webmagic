package cn.mc.scheduler.crawler.toutiao.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsContentArticleCoreManager;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.CommonUtil;
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

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @auther sin
 * @time 2018/3/8 15:42
 */
@Service
public class EntertainmentCrawler2Pipeline {

    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsImageMapper newsImageMapper;
    @Autowired
    private NewsContentArticleMapper newsContentArticleMapper;
    @Autowired
    private NewsContentArticleCoreManager newsContentArticleCoreManager;
    @Resource
    private  AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private MQTemplate mqTemplate;
    @Autowired
    private CrawlerManager crawlerManager;
    @Autowired
    private CrawlerUtil crawlerUtil;
    private static int size = 0;

    @Transactional
    public synchronized void saveEntertainment(String dataKey,
                                               Map<String, NewsDO> cacheNewsDO,
                                               Map<String, List<NewsImageDO>> cacheNewsImageDO,
                                               String articleContent) {

        // ?????????????????? return false
        if (!cacheNewsDO.containsKey(dataKey)) {
            return;
        }

        // ????????????
        NewsDO newsDO = cacheNewsDO.get(dataKey);

        // ????????????
        articleContent = SchedulerUtils.contentFilter(
                articleContent, newsDO.getNewsSource(),newsDO.getTitle(),newsDO.getNewsId());

        if (StringUtils.isEmpty(articleContent)) {
            return;
        }

        // ??????????????????????????? ??????
        NewsDO dataBaseNewsDO = crawlerManager.getNewsDOByDataKey(
                newsDO.getDataKey(), new Field("newsId", "newsType"));
        if (dataBaseNewsDO != null) {
            return;
        }

        // ??????????????????
        List<NewsImageDO> newsImageDOList = cacheNewsImageDO.get(dataKey);

        // build ??????????????????
        NewsContentArticleDO contentArticleDO = newsContentArticleCoreManager
                .buildNewsContentArticleDO(IDUtil.getNewID(), newsDO.getNewsId(), articleContent);
        //??????????????????????????????????????????
        contentArticleDO.setArticle(aliyunOSSClientUtil.replaceSourcePicToOSS(contentArticleDO.getArticle()));
        newsContentArticleMapper.insert(Update.copyWithoutNull(contentArticleDO));
        newsDO.setNewsState(NewsDO.STATE_NOT_RELEASE);
        newsMapper.insert(Update.copyWithoutNull(newsDO));
        //???????????????????????? ????????????
        crawlerUtil.addNewsTime(this.getClass().getSimpleName()+newsDO.getNewsType());

        // ?????? image
        for (NewsImageDO newsImageDO : newsImageDOList) {
            //??????????????????????????????????????????
            newsImageDO.setImageUrl(aliyunOSSClientUtil.replaceSourcePicToOSS(newsImageDO.getImageUrl()));
            newsImageMapper.insert(Update.copyWithoutNull(newsImageDO));
        }

        // ??????mq
        mqTemplate.sendNewsReviewMessage(MQTemplate.ARTICLE_TAG,
                ImmutableMap.of("newsId", newsDO.getNewsId()));

        CommonUtil.debug("\r\n \r\n size --------> " + ++size);
    }
}
