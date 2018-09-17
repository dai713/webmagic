package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.wa5.com.Wa5NewsCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Wa5NewsJob extends BaseJob {

    @Autowired
    private Wa5NewsCrawler wa5NewsCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        wa5NewsCrawler.createCrawler().thread(1).run();
    }
}
