package cn.mc.scheduler.crawler;

import cn.mc.scheduler.SchedulerApplication;
import cn.mc.scheduler.review.AutomaticReviewService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class AutomaticReviewTest {
    @Autowired
    private AutomaticReviewService automaticReviewService;
    @Test
    public void startSearchNewsCrawlerTest() {
        automaticReviewService.reviewNewsImgVideo();
    }
}
