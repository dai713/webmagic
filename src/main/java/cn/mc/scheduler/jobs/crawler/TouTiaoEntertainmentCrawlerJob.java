package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.toutiao.com.EntertainmentCrawler2;
import cn.mc.scheduler.crawler.toutiao.com.EntertainmentCrawler3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @auther sin
 * @time 2018/3/24 15:12
 */
@Component
public class TouTiaoEntertainmentCrawlerJob extends BaseJob {

    @Autowired
    private EntertainmentCrawler3 entertainmentCrawler;

//    @Scheduled(cron= "0 0/3 * * * ?")
    @Override
    public void execute() throws SchedulerNewException {
        entertainmentCrawler.createCrawler().thread(2).run();
    }
}
