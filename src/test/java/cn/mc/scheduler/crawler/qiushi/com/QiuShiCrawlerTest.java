package cn.mc.scheduler.crawler.qiushi.com;

import cn.mc.scheduler.SchedulerApplication;
import cn.mc.scheduler.crawler.qiushibaike.com.QiuShiCrawler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 糗事百科 test
 *
 * @auther sin
 * @time 2018/3/6 15:57
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class QiuShiCrawlerTest {

    @Autowired
    private QiuShiCrawler qiuShiCrawler;

    @Test
    public void qiuShiCrawlerTest() {
        qiuShiCrawler.createCrawler().thread(5).run();
    }
}
