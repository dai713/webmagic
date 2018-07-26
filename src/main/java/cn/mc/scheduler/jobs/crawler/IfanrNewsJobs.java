package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.ifanr.com.IfanrCrawlers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 爱范儿新闻定时任务
 *
 * @author daiqingwen
 * @date 2018-7-25 下午14:38
 */
@Component
public class IfanrNewsJobs extends BaseJob {
    @Autowired
    private IfanrCrawlers ifanrCrawlers;

    @Override
    public void execute() throws SchedulerNewException {
        ifanrCrawlers.createCrawler().thread(1).run();
    }
}
