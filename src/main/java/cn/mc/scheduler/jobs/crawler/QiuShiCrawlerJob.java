package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.qiushibaike.com.QiuShiCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @auther sin
 * @time 2018/3/24 15:13
 */
@Component
public class QiuShiCrawlerJob extends BaseJob {

    @Autowired
    private QiuShiCrawler qiuShiCrawler;

//    @Scheduled(cron= "0 0/3 * * * ?")
    @Override
    public void execute() throws SchedulerNewException {
        qiuShiCrawler.createCrawler().thread(5).run();
    }
}
