package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.kr.com.KrTechnologyNewsCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 36氪 job
 *
 * @author sin
 * @time 2018/7/23 16:31
 */
@Component
public class KrTechnologyNewsJob extends BaseJob {

    @Autowired
    private KrTechnologyNewsCrawler krTechnologyNewsCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        krTechnologyNewsCrawler.createCrawler().thread(2).run();
    }
}
