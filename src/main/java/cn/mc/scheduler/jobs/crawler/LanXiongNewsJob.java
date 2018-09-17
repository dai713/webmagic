package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.lanxiongsports.com.LanXiongSportsCrawlers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 懒熊新闻定时任务
 *
 * @author daiqingwen
 * @date 2018-7-25 下午14:38
 */
@Component
public class LanXiongNewsJob extends BaseJob {

    @Autowired
    private LanXiongSportsCrawlers lanXiongSportsCrawlers;


    @Override
    public void execute() throws SchedulerNewException {
        lanXiongSportsCrawlers.createCrawler().thread(1).run();
    }
}
