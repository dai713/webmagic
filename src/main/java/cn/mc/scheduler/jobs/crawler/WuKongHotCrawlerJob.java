package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.wukong.com.HotCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @auther sin
 * @time 2018/3/24 14:57
 */
@Component
public class WuKongHotCrawlerJob extends BaseJob {

    @Autowired
    private HotCrawler hotCrawler;

//    @Scheduled(cron= "0 0/3 * * * ?")
    @Override
    public void execute() throws SchedulerNewException {
        hotCrawler.createCrawler().thread(5).run();
    }
}
