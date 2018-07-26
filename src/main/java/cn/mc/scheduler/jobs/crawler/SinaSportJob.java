package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.sina.cn.SinaSportCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author sin
 * @time 2018/6/26 11:08
 */
@Component
public class SinaSportJob extends BaseJob {

    @Autowired
    private SinaSportCrawler sinaSportCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        sinaSportCrawler.createCrawler().thread(3).run();
    }
}
