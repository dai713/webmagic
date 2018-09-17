package cn.mc.scheduler.jobs.crawlerOverseas.yahoo.com.finance;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawlerOverseas.yahoo.com.finance.YahooFinanceCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * yahoo 财经新闻
 *
 * Created by dai on 2018/9/11.
 */
@Component
public class YahooFinanceJob extends BaseJob {

    @Autowired
    private YahooFinanceCrawler yahooFinanceCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        yahooFinanceCrawler.createCrawler().thread(2).run();
    }
}
