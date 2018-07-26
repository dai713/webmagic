package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.ithome.com.ITHomeNewsCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ITHomeCrawlerJob extends BaseJob {
    @Autowired
    private ITHomeNewsCrawler itHomeNewsCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        itHomeNewsCrawler.createCrawler().thread(3).run();
    }
}
