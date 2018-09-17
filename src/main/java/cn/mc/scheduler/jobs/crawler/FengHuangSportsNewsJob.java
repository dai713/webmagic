package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.i.ifeng.com.FengHuangSportNewsCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 凤凰 体育
 *
 * @auther sin
 * @time 2018/8/3 14:19
 */
@Component
public class FengHuangSportsNewsJob extends BaseJob {

    @Autowired
    private FengHuangSportNewsCrawler ifengSportNewsCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        ifengSportNewsCrawler.createCrawler().thread(2).run();
    }
}
