package cn.mc.scheduler.crawler.wx.qq.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class QQMilitaryNewsCrawlerTest {

    @Autowired
    private QQMilitaryNewsCrawler qqMilitaryNewsCrawler;

    @Test
    public void startTest() {
        qqMilitaryNewsCrawler.createCrawler().thread(1).run();
    }
}
