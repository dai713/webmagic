package cn.mc.scheduler.crawler.wangyi.com;

import cn.mc.core.dataObject.NewsContentVideoDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.CollectionUtil;
import cn.mc.scheduler.crawler.CrawlerManager;
import cn.mc.scheduler.mapper.NewsContentVideoMapper;
import cn.mc.scheduler.mapper.NewsImageMapper;
import cn.mc.scheduler.mapper.NewsMapper;
import cn.mc.scheduler.mq.MQTemplate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 网易视频 - 保存视频
 * @author daiqingwen
 * @date 2018-6-21 下午18:21
 */

@Component
public class WangYiVideoPipeline {


    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private NewsImageMapper newsImageMapper;
    @Autowired
    private NewsContentVideoMapper videoMapper;
    @Autowired
    private MQTemplate mqTemplate;
    @Autowired
    private CrawlerManager crawlerManager;

    /**
     * 保存数据
     *
     * @param newsDOMap
     * @param newsImageDOMap
     * @param videoDOMap
     */
    public void save(Map<Long, NewsDO> newsDOMap,
                     Map<Long, NewsImageDO> newsImageDOMap,
                     Map<Long, NewsContentVideoDO> videoDOMap) {


        // 统一去数据库检查，是否存在
        List<NewsDO> newsDOList = Lists.newArrayList(newsDOMap.values());
        Set<String> dataKeys = CollectionUtil.buildSet(
                newsDOList, String.class, "dataKey");

        List<NewsDO> dataKeyNewsDOList
                = crawlerManager.listNewsDOByDataKeys(
                        dataKeys, new Field("dataKey"));

        Map<String, NewsDO> dataKeyNewsDOMap = CollectionUtil.buildMap(
                dataKeyNewsDOList, String.class, NewsDO.class, "dataKey");

        // 开始单个保存
        for (NewsDO newsDO : newsDOList) {
            Long newsId = newsDO.getNewsId();

            // 数据库如果存在直接 continue
            if (dataKeyNewsDOMap.containsKey(newsId)) {
                continue;
            }

            if (!newsImageDOMap.containsKey(newsId)
                    || !videoDOMap.containsKey(newsId))
                continue;

            NewsImageDO newsImageDO = newsImageDOMap.get(newsId);
            NewsContentVideoDO videoDO = videoDOMap.get(newsId);

            // 去 save
            toSave(newsDO, newsImageDO, videoDO);

            // 发送mq
            mqTemplate.sendNewsReviewMessage(MQTemplate.VIDEO_TAG,
                    ImmutableMap.of("newsId", newsDO.getNewsId()));
        }
    }


    /**
     * 去保存数据
     *
     * @param newsDO
     * @param newsImageDO
     * @param videoDO
     */
    @Transactional
    public synchronized void toSave(@NotNull NewsDO newsDO,
                                    @NotNull NewsImageDO newsImageDO,
                                    @NotNull NewsContentVideoDO videoDO) {
        newsMapper.insert(Update.copyWithoutNull(newsDO));
        newsImageMapper.insert(Update.copyWithoutNull(newsImageDO));
        videoMapper.insert(Update.copyWithoutNull(videoDO));
    }
}
