package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.engadget.com.EngadgetCrawlers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/**
 * engadget 新闻定时任务
 *
 * @author daiqingwen
 * @date 2018-7-25 下午14:38
 */
@Component
public class EngadgetNewsJob extends BaseJob {

    @Autowired
    private EngadgetCrawlers engadgetCrawlers;

    @Override
    public void execute() throws SchedulerNewException {
        engadgetCrawlers.createCrawler().thread(1).run();
    }
}
