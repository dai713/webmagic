package cn.mc.scheduler.crawler.kr.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 36氪 test
 *
 * @author sin
 * @time 2018/7/23 15:17
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class KrTechnologyNewsCrawlerTest {

    @Autowired
    private KrTechnologyNewsCrawler krTechnologyNewsCrawler;

    @Test
    public void KrTechnologyNewsCrawlerTest() {
        krTechnologyNewsCrawler.createCrawler().thread(2).run();
    }
}
