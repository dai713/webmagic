package cn.mc.scheduler.crawlerOverseas.ReadWrite.com;

import cn.mc.scheduler.SchedulerApplication;
import cn.mc.scheduler.crawlerOverseas.readwrite.com.ReadWriteAiCrawler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * ReadWrite.com - ai test
 *
 * @author Sin
 * @time 2018/8/29 上午9:26
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class ReadWriteAiCrawlerTest {

    @Autowired
    private ReadWriteAiCrawler readWriteAiCrawler;

    @Test
    public void readWriteAiCrawlerTest() {
        readWriteAiCrawler.createCrawler().thread(1).run();
    }
}
