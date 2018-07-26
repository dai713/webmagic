package cn.mc.scheduler;

import cn.mc.scheduler.active.MailService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @auther sin
 * @time 2018/2/3 09:42
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class SendMailTest {

    @Autowired
    private MailService mailService;

    @Test
    public void sendMy() {
        mailService.sendSystemFailMessage("ceshi");
    }
}
