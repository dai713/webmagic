package cn.mc.scheduler.crawler.news.baidu.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @auther sin
 * @time 2018/3/8 10:20
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class RecommendCrawlerTest {

    @Autowired
    private SearchNewsCrawler searchNewsCrawler;

    @Test
    public void startSearchNewsCrawlerTest() {
//        while (true) {
            searchNewsCrawler.createCrawler().thread(1).run();
//        }
    }
}
