package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.news.baidu.com.BaiDuEntertainmentCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author sin
 * @time 2018/7/20 14:41
 */
@Component
public class BaiDuEntertainmentJob extends BaseJob {

    @Autowired
    private BaiDuEntertainmentCrawler baiDuEntertainmentCrawler;

    @Override
    public void execute() throws SchedulerNewException {
        baiDuEntertainmentCrawler.createCrawler().thread(2).run();
    }
}
