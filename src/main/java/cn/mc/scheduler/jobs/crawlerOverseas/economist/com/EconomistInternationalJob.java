package cn.mc.scheduler.jobs.crawlerOverseas.economist.com;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawlerOverseas.economist.com.EconomistInternationalCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * www.economist.com 海外财经 - 国际
 *
 * @author Sin
 * @time 2018/9/14 下午2:13
 */
@Component
public class EconomistInternationalJob extends BaseJob {

    @Autowired
    private EconomistInternationalCrawler economistInternationalCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        economistInternationalCrawler.createCrawler().thread(2).run();
    }
}
