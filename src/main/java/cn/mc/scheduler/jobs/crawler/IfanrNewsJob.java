package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.ifanr.com.IfanrCrawlers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 爱范儿网新闻
 *
 * @author Sin
 * @time 2018/8/9 下午5:34
 */
@Component
public class IfanrNewsJob extends BaseJob {

    @Autowired
    private IfanrCrawlers ifanrCrawlers;

    @Override
    public void execute() throws SchedulerNewException {
        ifanrCrawlers.createCrawler().thread(1).run();
    }
}
