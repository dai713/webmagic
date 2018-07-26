package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.wangyi.com.WangYiEntertainmentCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author sin
 * @time 2018/7/20 17:26
 */
@Component
public class WangYiEntertainmentJob extends BaseJob {

    @Autowired
    private WangYiEntertainmentCrawler wangYiEntertainmentCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        wangYiEntertainmentCrawler.createCrawler().thread(2).run();
    }
}
