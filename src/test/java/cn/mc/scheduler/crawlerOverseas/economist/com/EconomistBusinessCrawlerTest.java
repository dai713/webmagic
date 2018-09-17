package cn.mc.scheduler.crawlerOverseas.economist.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * www.economist.com 海外财经 - 商业
 *
 * @author Sin
 * @time 2018/9/14 下午2:29
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class EconomistBusinessCrawlerTest {

    @Autowired
    private EconomistBusinessCrawler economistBusinessCrawler;

    @Test
    public void economistNewestCrawlerTest() {
        economistBusinessCrawler.createCrawler().thread(1).run();
    }
}
