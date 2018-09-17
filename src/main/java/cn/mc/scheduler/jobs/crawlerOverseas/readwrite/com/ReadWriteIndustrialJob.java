package cn.mc.scheduler.jobs.crawlerOverseas.readwrite.com;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawlerOverseas.readwrite.com.ReadWriteIndustrialCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * readwrite.com - 产业 爬虫
 *
 * @author Sin
 * @time 2018/8/28 下午8:11
 */
@Component
public class ReadWriteIndustrialJob extends BaseJob {

    @Autowired
    private ReadWriteIndustrialCrawler readWriteIndustrialCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        readWriteIndustrialCrawler.createCrawler().thread(2).run();
    }
}
