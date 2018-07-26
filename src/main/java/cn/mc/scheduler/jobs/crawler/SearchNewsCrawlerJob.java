package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.news.baidu.com.SearchNewsCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @auther sin
 * @time 2018/3/24 15:13
 */
@Component
public class SearchNewsCrawlerJob extends BaseJob {

    @Autowired
    private SearchNewsCrawler searchNewsCrawler;

//    @Scheduled(cron= "0 0/3 * * * ?")
    @Override
    public void execute() throws SchedulerNewException {
        searchNewsCrawler.createCrawler().thread(5).run();
    }
}
