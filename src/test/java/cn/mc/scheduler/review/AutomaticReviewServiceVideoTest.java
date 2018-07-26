package cn.mc.scheduler.review;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author sin
 * @time 2018/6/29 10:11
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class AutomaticReviewServiceVideoTest {

    @Autowired
    private AutomaticReviewVideoService automaticReviewVideoService;

    @Test
    public void reviewNewsImgVideoTest() {
        automaticReviewVideoService.reviewNewsImgVideo();
    }
}
