package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.wx.qq.com.QQTechnologyNewsCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QQTechnologyNewsJob extends BaseJob {

    @Autowired
    private QQTechnologyNewsCrawler qqTechnologyNewsCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        qqTechnologyNewsCrawler.createCrawler().thread(3).run();
    }
}
