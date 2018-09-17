package cn.mc.scheduler.crawlerOverseas.economist.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * www.economist.com 海外财经 - 国际 test
 *
 * @author Sin
 * @time 2018/9/14 下午3:43
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class EconomistInternationalCrawlerTest {

    @Autowired
    private EconomistInternationalCrawler economistInternationalCrawler;

    @Test
    public void economistInternationalCrawlerTest() {
        economistInternationalCrawler.createCrawler().thread(1).run();
    }
}
