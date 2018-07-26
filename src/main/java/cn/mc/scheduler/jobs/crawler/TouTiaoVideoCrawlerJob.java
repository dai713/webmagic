package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.toutiao.com.VideoCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @auther sin
 * @time 2018/3/24 14:57
 */
@Component
public class TouTiaoVideoCrawlerJob extends BaseJob {

    @Autowired
    private VideoCrawler videoCrawler;

//    @Scheduled(cron= "0 0/3 * * * ?")
    @Override
    public void execute() throws SchedulerNewException {
        videoCrawler.createCrawler().thread(5).run();
    }
}
