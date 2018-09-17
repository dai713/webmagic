package cn.mc.scheduler.jobs.crawlerOverseas.yahoo.com.sport;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawlerOverseas.yahoo.com.sport.YahooSportGolfCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * YahooSport - 高尔夫球新闻
 *
 * Created by dai on 2018/9/12.
 */
@Component
public class YahooSportGolfJob extends BaseJob {

    @Autowired
    private YahooSportGolfCrawler yahooSportGolfCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        yahooSportGolfCrawler.createCrawler().thread(2).run();
    }
}
