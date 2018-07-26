package cn.mc.scheduler.crawler.wx.qq.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * QQ 体育
 *
 * @author sin
 * @time 2018/7/13 13:45
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class QQSportNewsCrawlerTest {

    @Autowired
    private QQSportNewsCrawler qqSportNewsCrawler;

    @Test
    public void startTest() {
        qqSportNewsCrawler.createCrawler().thread(1).run();
    }
}
