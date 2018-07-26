package cn.mc.scheduler.crawler.www.alexa.cn;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 海外 url 抓取
 *
 * @auther sin
 * @time 2018/3/10 16:14
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class UrlCrawlerTest {

    @Autowired
    private UrlCrawler urlCrawler;

    @Test
    public void startTest() {
        urlCrawler.createCrawler().thread(1).run();
    }

}
