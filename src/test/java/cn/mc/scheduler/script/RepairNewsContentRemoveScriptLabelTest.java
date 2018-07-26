package cn.mc.scheduler.script;

import cn.mc.scheduler.SchedulerApplication;
import cn.mc.scheduler.script.news.RepairNewsContentRemoveLabelScript;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @auther sin
 * @time 2018/3/27 10:16
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class RepairNewsContentRemoveScriptLabelTest {

    @Autowired
    private RepairNewsContentRemoveLabelScript repairNewsContentRemoveScriptLabel;

    @Test
    public void startTest() {
        repairNewsContentRemoveScriptLabel.script();
    }
}
