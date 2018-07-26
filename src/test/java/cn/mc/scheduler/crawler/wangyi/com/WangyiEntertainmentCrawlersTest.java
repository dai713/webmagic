package cn.mc.scheduler.crawler.wangyi.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


/**
 * 网易娱乐 test
 *
 * @auther sin
 * @time 2018/3/8 15:42
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class WangYiEntertainmentCrawlersTest {

    @Autowired
    private WangYiEntertainmentCrawler wangYiEntertainmentCrawler;

    @Test
    public void startTest() {
        wangYiEntertainmentCrawler.createCrawler().thread(1).run();
    }
}