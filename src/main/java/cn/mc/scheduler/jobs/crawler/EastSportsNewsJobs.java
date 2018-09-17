package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.sports.eastday.com.EastSportsCrawlers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 东方体育新闻定时任务
 *
 * @author daiqingwen
 * @date 2018-7-30 下午18:17
 */
@Component
public class EastSportsNewsJobs extends BaseJob {

    @Autowired
    private EastSportsCrawlers eastSportsCrawlers;

    @Override
    public void execute() throws SchedulerNewException {
        eastSportsCrawlers.createCrawler().thread(1).run();
    }
}
