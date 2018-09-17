package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.wangyi.com.WangYiMilitaryCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WangYiMilitaryJob extends BaseJob {

    @Autowired
    private WangYiMilitaryCrawler wangYiMilitaryCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        wangYiMilitaryCrawler.createCrawler().thread(1).run();
    }
}
