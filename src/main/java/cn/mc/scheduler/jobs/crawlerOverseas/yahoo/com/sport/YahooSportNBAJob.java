package cn.mc.scheduler.jobs.crawlerOverseas.yahoo.com.sport;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawlerOverseas.yahoo.com.sport.YahooSportNBACrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * YahooSport - NBA新闻
 *
 * Created by dai on 2018/9/12.
 */
@Component
public class YahooSportNBAJob extends BaseJob {

    @Autowired
    private YahooSportNBACrawler yahooSportNBACrawler;

    @Override
    public void execute() throws SchedulerNewException {
        yahooSportNBACrawler.createCrawler().thread(2).run();
    }
}
