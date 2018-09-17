package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.santaihu.com.SantaihuNewsCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SantaihuJob extends BaseJob {

    @Autowired
    private SantaihuNewsCrawler santaihuNewsCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        santaihuNewsCrawler.createCrawler().thread(1).run();
    }
}
