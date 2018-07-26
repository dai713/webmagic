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

    }

    public static void main(String[] args) {
//        String ss = "dsfasf\nlkjlkjkljk\r666666666666\neroiuewroewquri\r00000";

//        String rgx = "[\r\n]";
//        String[] arr = ss.split(rgx);
//        for (int i = 0; i < arr.length; i++) {
//            System.out.println(arr[i]);
//        }
//        String text = "auser1 home1b\r" +
//                "auser2 home2b\n" +
//                "auser3 home3b";
//        Matcher m = Pattern.compile(rgx).matcher(ss);
//        while (m.find()) {
//            System.out.println(m.group());
//        }


    }
}
