package cn.mc.scheduler.crawler.wangyi.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author sin
 * @time 2018/7/20 15:19
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class WangYiEntertainmentCrawlerTest {

    @Autowired
    private WangYiEntertainmentCrawler wangYiEntertainmentCrawler;

    @Test
    public void wangYiEntertainmentCrawlerTest() {
        wangYiEntertainmentCrawler.createCrawler().thread(2).run();
    }
}
