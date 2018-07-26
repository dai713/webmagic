package cn.mc.scheduler.crawler.wx.qq.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * QQ 娱乐
 *
 * @author sin
 * @time 2018/7/25 16:23
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class QQEntertainmentCrawlerTest {

    @Autowired
    private QQEntertainmentCrawler qqEntertainmentCrawler;

    @Test
    public void qqEntertainmentCrawlerTest() {
        qqEntertainmentCrawler.createCrawler().thread(2).run();
    }
}
