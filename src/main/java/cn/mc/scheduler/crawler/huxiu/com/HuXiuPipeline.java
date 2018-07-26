package cn.mc.scheduler.crawler.huxiu.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.scheduler.mapper.NewsContentArticleMapper;
import cn.mc.scheduler.mapper.NewsImageMapper;
import cn.mc.scheduler.mapper.NewsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 虎嗅网新闻 - 保存
 *
 * @author daiqingwen
 * @date 2018-7-23 下午19:09
 */

@Component
public class HuXiuPipeline {
    @Autowired
    private NewsContentArticleMapper newsContentArticleMapper;

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private NewsImageMapper newsImageMapper;

    /**
     * 保存新闻
     * @param list
     * @param detailList
     */
    @Transactional
    public synchronized void save(List<NewsDO> list, List<NewsContentArticleDO> detailList, List<NewsImageDO> imageList) {

        newsMapper.insertNews(list);

        newsImageMapper.insertNewsImage(imageList);

        newsContentArticleMapper.insertArticleList(detailList);

    }

}
