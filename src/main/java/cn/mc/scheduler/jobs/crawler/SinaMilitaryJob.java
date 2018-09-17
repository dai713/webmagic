package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.sina.cn.SinaMilitaryCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SinaMilitaryJob extends BaseJob {

    @Autowired
    private SinaMilitaryCrawler sinaMilitaryCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        try {
            sinaMilitaryCrawler.createCrawler().thread(2).run();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
