package cn.mc.scheduler.jobs.crawler;


import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.geekpark.com.GeekParkCrawlers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 极客公园新闻定时任务
 *
 * @author daiqingwen
 * @date 2018-7-25 下午14:48
 */

@Component
public class GeekParkNewsJob extends BaseJob {

    @Autowired
    private GeekParkCrawlers geekParkCrawlers;

    @Override
    public void execute() throws SchedulerNewException {
        geekParkCrawlers.createCrawler().thread(1).run();
    }
}
