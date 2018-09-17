package cn.mc.scheduler.crawlerOverseas.ReadWrite.com;

import cn.mc.scheduler.SchedulerApplication;
import cn.mc.scheduler.crawlerOverseas.readwrite.com.ReadWriteIndustrialCrawler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * ReadWrite.com - 产业 test
 *
 * @author Sin
 * @time 2018/9/14 上午9:05
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class ReadWriteIndustrialCrawlerTest {

    @Autowired
    private ReadWriteIndustrialCrawler readWriteIndustrialCrawler;

    @Test
    public void readWriteIndustrialCrawlerTest() {
        readWriteIndustrialCrawler.createCrawler().thread(1).run();
    }
}
