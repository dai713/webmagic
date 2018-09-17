package cn.mc.scheduler.jobs.crawlerOverseas.yahoo.com.sport;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawlerOverseas.yahoo.com.sport.YahooSportFootballCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * YahooSport - 足球新闻
 *
 * Created by dai on 2018/9/12.
 */
@Component
public class YahooSportFootballJob extends BaseJob {

    @Autowired
    private YahooSportFootballCrawler yahooSportFootballCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        yahooSportFootballCrawler.createCrawler().thread(2).run();
    }
}
