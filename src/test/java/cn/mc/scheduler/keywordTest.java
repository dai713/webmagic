package cn.mc.scheduler;


import cn.mc.scheduler.util.SchedulerUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class keywordTest {

    @Autowired
    private SchedulerUtils schedulerUtils;

    @Test
    public void replace() {
//        String content = "轻松一刻：班长总是打我，我让胶水涂在他位子上，当天";
//        Integer type = 0;
//        Long newsId = 32423413234234234L;
//        boolean flag = schedulerUtils.keywordsMatch(content, type, newsId);
//        System.out.println(flag);
    }

}
