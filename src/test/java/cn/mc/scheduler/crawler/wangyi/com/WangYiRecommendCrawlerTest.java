package cn.mc.scheduler.crawler.wangyi.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 网易推荐 test
 *
 * @author sin
 * @time 2018/7/23 10:23
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class WangYiRecommendCrawlerTest {

    @Autowired
    private WangYiRecommendCrawler wangYiRecommendCrawler;

    @Test
    public void wangYiRecommendCrawlerTest() {
        wangYiRecommendCrawler.createCrawler().thread(2).run();
    }
}
