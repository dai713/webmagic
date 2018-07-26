package cn.mc.scheduler.review.rest;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

/**
 * @author sin
 * @time 2018/7/11 20:34
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class CheckVideoClientTest {

    @Autowired
    private CheckVideoClient checkVideoClient;

    @Test
    public void checkVideoUrlTest() {
        String videoUrl = "http://v11-tt.ixigua.com/99dc8c87552d648e12c778108cd0033f/5b45fc3c/video/m/2202cc98b25656244978acc69b9bd64fd1b1159395300000bc47b85199d/";
        boolean reviewResult = checkVideoClient.checkVideoUrl(videoUrl);
        Assert.isTrue(reviewResult, "视频审核不成功!");
    }
}