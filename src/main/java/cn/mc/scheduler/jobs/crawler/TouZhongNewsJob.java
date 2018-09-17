package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.chinaventure.com.cn.TouZhongNewsCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TouZhongNewsJob extends BaseJob {

    @Autowired
    private TouZhongNewsCrawler touZhongNewsCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        touZhongNewsCrawler.createCrawler().thread(1).run();
    }
}
