package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.toutiao.com.ToutiaoPictureCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 抓取头条 - 图片新闻定时任务
 * @author daiqingwen
 * @date 2018-6-8 下午18:09
 */
@Component
public class ToutiaoPictureCrawlersJob extends BaseJob {

    @Autowired
    private ToutiaoPictureCrawler pictureCrawler;

//    @Scheduled(cron= "0 0/6 * * * ?")
    @Override
    public void execute() throws SchedulerNewException {
        pictureCrawler.createCrawler().thread(3).run();
    }
}
