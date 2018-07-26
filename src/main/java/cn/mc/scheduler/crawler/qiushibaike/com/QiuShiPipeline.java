package cn.mc.scheduler.crawler.qiushibaike.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsSectionUserDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.DateUtil;
import cn.mc.scheduler.crawler.BasePipeline;
import cn.mc.scheduler.crawler.CrawlerManager;
import cn.mc.scheduler.mapper.NewsContentArticleMapper;
import cn.mc.scheduler.mapper.NewsMapper;
import cn.mc.scheduler.mapper.NewsSectionUserMapper;
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;

import javax.annotation.Resource;
import java.util.Calendar;

/**
 * @auther sin
 * @time 2018/3/7 14:33
 */
@Component
@Deprecated
public class QiuShiPipeline extends BasePipeline {

    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsContentArticleMapper newsContentArticleMapper;
    @Autowired
    private NewsSectionUserMapper newsSectionUserMapper;
    @Resource
    private  AliyunOSSClientUtil aliyunOSSClientUtil;
    @Autowired
    private CrawlerManager crawlerManager;

    @Override
    @Transactional
    protected void doProcess(ResultItems resultItems, Task task) {
        NewsDO newsDO = resultItems.get(QiuShiCrawler.RESULT_ITEM_NEWS_DO_KEY);

        NewsSectionUserDO newsSectionUserDO
                = resultItems.get(QiuShiCrawler.RESULT_ITEM_NEWS_SECTION_USER_KEY);

        NewsContentArticleDO newsContentArticleDO
                = resultItems.get(QiuShiCrawler.RESULT_ITEM_NEWS_CONTENT_TEXT_DO_KEY);


        NewsDO dataBaseNewsDO = crawlerManager.listNewsDOByDataKey(
                newsDO.getDataKey(), new Field("newsId"));
        
        if (dataBaseNewsDO != null) {
            return;
        }

        newsDO.setNewsState(NewsDO.STATE_NOT_RELEASE);
        newsMapper.insert(Update.copyWithoutNull(newsDO));
        //上传阿里云替换成我们图片地址
        newsSectionUserDO.setAvatarUrl(aliyunOSSClientUtil.replaceSourcePicToOSS(newsSectionUserDO.getAvatarUrl()));
        newsSectionUserMapper.insert(Update.copyWithoutNull(newsSectionUserDO));
        //上传阿里云替换成我们图片地址
        newsContentArticleDO.setArticle(aliyunOSSClientUtil.replaceSourcePicToOSS(newsContentArticleDO.getArticle()));
        newsContentArticleMapper.insert(Update.copyWithoutNull(newsContentArticleDO));
    }
}
