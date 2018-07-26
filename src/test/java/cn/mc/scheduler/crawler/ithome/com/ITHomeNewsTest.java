package cn.mc.scheduler.crawler.ithome.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class ITHomeNewsTest {

    @Autowired
    private ITHomeNewsCrawler itHomeNewsCrawler;

    @Test
    public void startTest() {
        itHomeNewsCrawler.createCrawler().thread(1).run();
    }
}
