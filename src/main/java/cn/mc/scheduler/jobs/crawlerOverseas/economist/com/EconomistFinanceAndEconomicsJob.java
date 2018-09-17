package cn.mc.scheduler.jobs.crawlerOverseas.economist.com;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawlerOverseas.economist.com.EconomistFinanceAndEconomicsCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * www.economist.com 海外财经 - 财经
 *
 * @author Sin
 * @time 2018/9/14 下午4:35
 */
@Component
public class EconomistFinanceAndEconomicsJob extends BaseJob {

    @Autowired
    private EconomistFinanceAndEconomicsCrawler economistFinanceAndEconomicsCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        economistFinanceAndEconomicsCrawler.createCrawler().thread(2).run();
    }
}
