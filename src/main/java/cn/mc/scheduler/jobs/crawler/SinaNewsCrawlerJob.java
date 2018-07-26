package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.sina.cn.SinaNewsCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SinaNewsCrawlerJob extends BaseJob {

    @Autowired
    private SinaNewsCrawler sinaNewsCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        sinaNewsCrawler.createCrawler().thread(3).run();
    }
}
