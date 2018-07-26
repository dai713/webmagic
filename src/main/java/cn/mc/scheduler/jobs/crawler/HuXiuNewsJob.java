package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.huxiu.com.HuXiuCrawlers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 虎嗅新闻定时任务
 *
 * @author daiqingwen
 * @date 2018-7-25 下午14:34
 */
@Component
public class HuXiuNewsJob extends BaseJob {

    @Autowired
    private HuXiuCrawlers huXiuCrawlers;

    @Override
    public void execute() throws SchedulerNewException {
        huXiuCrawlers.createCrawler().thread(1).run();
    }
}
