package cn.mc.scheduler.crawler.wangyi.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class WangyiMilitaryCrewlersTest {
    @Autowired
    private WangYiMilitaryCrawler wangyiMilitaryCrewlers;
    @Test
    public void startTest() {
        wangyiMilitaryCrewlers.createCrawler().thread(1).run();
    }
}
