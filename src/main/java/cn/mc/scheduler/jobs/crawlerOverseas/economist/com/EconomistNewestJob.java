package cn.mc.scheduler.jobs.crawlerOverseas.economist.com;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawlerOverseas.economist.com.EconomistNewestCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * www.economist.com 海外财经 - 最新栏目(最后更新)
 *
 * @author Sin
 * @time 2018/9/14 下午2:13
 */
@Component
public class EconomistNewestJob extends BaseJob {

    @Autowired
    private EconomistNewestCrawler economistNewestCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        economistNewestCrawler.createCrawler().thread(2).run();
    }
}
