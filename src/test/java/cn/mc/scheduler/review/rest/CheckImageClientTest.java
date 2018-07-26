package cn.mc.scheduler.review.rest;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author sin
 * @time 2018/7/11 20:34
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class CheckImageClientTest {

    @Autowired
    private CheckImgClient checkImgClient;

    @Test
    public void checkImgUrlTest() {
        String imageUrl = "http://browser-file.oss-cn-hangzhou.aliyuncs.com/img/1000000016607084544.png";
        checkImgClient.checkImgUrl(imageUrl);
    }
}