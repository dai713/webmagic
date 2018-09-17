package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.leiphone.com.LeiPhoneNewsCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 雷锋网
 *
 * @auther sin
 * @time 2018/8/3 14:19
 */
@Component
public class LeiPhoneNewsJob extends BaseJob {

    @Autowired
    private LeiPhoneNewsCrawler leiPhoneNewsCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        leiPhoneNewsCrawler.createCrawler().thread(1).run();
    }
}
