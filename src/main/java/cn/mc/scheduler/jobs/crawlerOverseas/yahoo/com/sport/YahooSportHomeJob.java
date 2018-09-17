package cn.mc.scheduler.jobs.crawlerOverseas.yahoo.com.sport;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawlerOverseas.yahoo.com.sport.YahooSportHomeCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * YahooSport - 体育之家新闻
 *
 * Created by dai on 2018/9/12.
 */
@Component
public class YahooSportHomeJob extends BaseJob {

    @Autowired
    private YahooSportHomeCrawler yahooSportHomeCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        yahooSportHomeCrawler.createCrawler().thread(2).run();
    }
}
