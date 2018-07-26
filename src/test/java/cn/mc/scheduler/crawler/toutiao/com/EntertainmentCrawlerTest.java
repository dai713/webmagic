package cn.mc.scheduler.crawler.toutiao.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @auther sin
 * @time 2018/3/8 15:44
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class EntertainmentCrawlerTest {

    @Autowired
    private EntertainmentCrawler3 entertainmentCrawler;
    @Autowired
    private TouTiaoRecommendCrawler  touTiaoRecommendCrawler;

    @Autowired
    private VideoCrawler videoCrawler;

    @Test
    public void touTiaoRecommendCrawlerTest() {
        touTiaoRecommendCrawler.createCrawler().thread(2).run();
    }

    @Test
    public void startTest() {
        entertainmentCrawler.createCrawler().thread(1).run();
    }

    @Test
    public void videoStartTest() {
        videoCrawler.createCrawler().thread(3).run();
//        for (int i = 0; i < 100; i++) {
//        }
    }
}
