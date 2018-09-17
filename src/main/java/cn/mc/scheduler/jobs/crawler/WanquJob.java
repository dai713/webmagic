package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.santaihu.com.SantaihuNewsCrawler;
import cn.mc.scheduler.crawler.wanqu.co.WanquNewsCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WanquJob extends BaseJob {

    @Autowired
    private WanquNewsCrawler wanquNewsCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        wanquNewsCrawler.createCrawler().thread(1).run();
    }
}
