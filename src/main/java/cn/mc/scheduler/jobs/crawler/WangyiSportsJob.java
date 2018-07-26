package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.wangyi.com.WangYiSprotCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author sin
 * @time 2018/6/26 11:08
 */
@Component
public class WangyiSportsJob extends BaseJob {

    @Autowired
    private WangYiSprotCrawler wangyiSprotCrawlers;

    @Override
    public void execute() throws SchedulerNewException {
        wangyiSprotCrawlers.createCrawler().thread(3).run();
    }
}
