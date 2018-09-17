package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.mydrivers.com.FastTechnologyNewsCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * 快科技
 *
 * @auther sin
 * @time 2018/8/3 14:19
 */
@Component
public class FastTechnologyNewsJob extends BaseJob {

    @Autowired
    private FastTechnologyNewsCrawler fastTechnologyNewsCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        fastTechnologyNewsCrawler.createCrawler().thread(1).run();
    }
}
