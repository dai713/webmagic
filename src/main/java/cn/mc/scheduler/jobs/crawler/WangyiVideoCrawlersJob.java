package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.wangyi.com.WangYiVideoCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 抓取网易视频定时任务
 * @author daiqingwen
 * @date 2018-6-8 下午17:59
 */
@Component
public class WangyiVideoCrawlersJob extends BaseJob {

    @Autowired
    private WangYiVideoCrawler wangyiVideoCrawlers;

//    @Scheduled(cron= "0 0/4 * * * ?")
    @Override
    public void execute()  throws SchedulerNewException {
        wangyiVideoCrawlers.createCrawler().thread(3).run();
    }
}
