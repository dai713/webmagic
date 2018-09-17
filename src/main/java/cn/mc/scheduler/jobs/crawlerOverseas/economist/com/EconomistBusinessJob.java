package cn.mc.scheduler.jobs.crawlerOverseas.economist.com;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawlerOverseas.economist.com.EconomistBusinessCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * www.economist.com 海外财经 - 商业
 *
 * @author Sin
 * @time 2018/9/14 下午2:13
 */
@Component
public class EconomistBusinessJob extends BaseJob {

    @Autowired
    private EconomistBusinessCrawler economistBusinessCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        economistBusinessCrawler.createCrawler().thread(2).run();
    }
}
