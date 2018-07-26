package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.toutiao.com.TouTiaoRecommendCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 抓取头条 - 推荐新闻定时任务
 * @author daiqingwen
 * @date 2018-6-8 下午18:09
 */
@Component
public class ToutiaoRecommendCrawlerJob extends BaseJob {

    @Autowired
    private TouTiaoRecommendCrawler recommendCrawler;

//    @Scheduled(cron= "0 0/7 * * * ?")
    @Override
    public void execute() throws SchedulerNewException {
        recommendCrawler.createCrawler().thread(3).run();
    }
}
