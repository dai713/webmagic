package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.pedaily.cn.PedailyNewsCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 投资界
 *
 * @auther sin
 * @time 2018/8/3 14:19
 */
@Component
public class PedailyNewsJob extends BaseJob {

    @Autowired
    private PedailyNewsCrawler pedailyNewsCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        pedailyNewsCrawler.createCrawler().thread(1).run();
    }
}
