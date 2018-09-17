package cn.mc.scheduler.jobs.crawlerOverseas.readwrite.com;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawlerOverseas.readwrite.com.ReadWriteAiCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * readwrite.com - ai 爬虫
 *
 * @author Sin
 * @time 2018/8/28 下午8:11
 */
@Component
public class ReadWriteAiJob extends BaseJob {

    @Autowired
    private ReadWriteAiCrawler readWriteAiCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        readWriteAiCrawler.createCrawler().thread(2).run();
    }
}
