package cn.mc.scheduler.crawler.wukong.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @auther sin
 * @time 2018/3/10 16:16
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class HotCrawlerTest {

    @Autowired
    private HotCrawler hotCrawler;

    @Test
    public void startTest() {
        hotCrawler.createCrawler().thread(1).run();
    }

}
