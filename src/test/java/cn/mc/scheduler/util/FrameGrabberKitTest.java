package cn.mc.scheduler.util;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class FrameGrabberKitTest {

    @Autowired
    private SchedulerUtils schedulerUtils;


    @Test
    public void grabber() {
        String url = "https://mc-browser.oss-cn-shenzhen.aliyuncs.com/img/1011442049997602816.png";
        String videoPath = "http://v11-tt.ixigua.com/08148928be86272dbc51b6bc6a74858b/5b35b527/video/m/2208d88fc1ce53d420f9806c102e8acffca11574a910000a7548d1d4e24/";
//        System.out.println(new SchedulerUtils().imageCheck(url));
//        System.out.println(schedulerUtils.videoCheck(videoPath));
    }

}