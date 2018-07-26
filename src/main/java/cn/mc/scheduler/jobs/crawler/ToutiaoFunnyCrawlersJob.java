package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.crawler.toutiao.com.ToutiaoFunnyCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 抓取头条 - 搞笑新闻定时任务
 * @author daiqingwen
 * @date 2018-6-8 下午18:06
 */
@Component
public class ToutiaoFunnyCrawlersJob {

    @Autowired
    private ToutiaoFunnyCrawler funnyCrawler;

//    @Scheduled(cron= "0 0/5 * * * ?")
    public void execute() throws SchedulerNewException {
        funnyCrawler.createCrawler().thread(3).run();
    }
}
