package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.feng.com.WeiFengCrawlers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 威锋网新闻定时任务
 *
 * @author daiqingwen
 * @date 2018-7-25 下午14:38
 */

@Component
public class WeiFengNewsJobs extends BaseJob {

    @Autowired
    private WeiFengCrawlers weiFengCrawlers;

    @Override
    public void execute() throws SchedulerNewException {
        weiFengCrawlers.createCrawler().thread(1).run();
    }
}
