package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.wangyi.com.WangYiRecommendCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author sin
 * @time 2018/7/23 14:24
 */
@Component
public class WangYiRecommendJob extends BaseJob {

    @Autowired
    private WangYiRecommendCrawler wangYiRecommendCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        wangYiRecommendCrawler.createCrawler().thread(2).run();
    }
}
