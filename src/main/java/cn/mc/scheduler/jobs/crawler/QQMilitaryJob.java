package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.wx.qq.com.QQMilitaryNewsCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QQMilitaryJob extends BaseJob {

    @Autowired
    private QQMilitaryNewsCrawler qqMilitaryNewsCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        qqMilitaryNewsCrawler.createCrawler().thread(1).run();
    }
}
