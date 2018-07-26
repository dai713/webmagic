package cn.mc.scheduler.jobs.crawler;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawler.weixin.sogou.com.WeiXinSoGouCrawler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/**
 * 搜狗-微信新闻定时任务
 *
 * @author daiqingwen
 * @date 2018-7-25 下午14:38
 */

@Component
public class WeiXinSouHuNewsJob extends BaseJob {

    @Autowired
    private WeiXinSoGouCrawler weiXinSoGouCrawler;


    @Override
    public void execute() throws SchedulerNewException {
        weiXinSoGouCrawler.createCrawler().thread(1).run();
    }
}
