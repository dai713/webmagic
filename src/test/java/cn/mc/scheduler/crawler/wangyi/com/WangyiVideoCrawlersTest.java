package cn.mc.scheduler.crawler.wangyi.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class WangyiVideoCrawlersTest {

    @Autowired
    private WangYiVideoCrawler videoCrawlers;

    @Test
    public void createCrawler() {
        videoCrawlers.createCrawler().thread(1).run();
    }
      



//    https://c.m.163.com/recommend/getChanListNews?channel=T1457068979049&passport=&devId=IIuuXb/hLUSPTntXq4kI%2BoRnb0c8Tfr%2BE7bHyJYx5KpjHUvhlNSkVu2N48qn/bQm&version=37.1&spever=false&net=wifi&lat=&lon=&ts=1528438782&sign=zREo9KopFt13hDOFqafmK/2kFOAo4Ae06gt1Dt6iGkt48ErR02zJ6/KXOnxX046I&encryption=1&canal=appstore&offset=0&size=10&fn=9
//    https://c.m.163.com/recommend/getChanListNews?channel=T1457068979049&passport=&devId=IIuuXb/hLUSPTntXq4kI%2BoRnb0c8Tfr%2BE7bHyJYx5KpjHUvhlNSkVu2N48qn/bQm&version=37.1&spever=false&net=wifi&lat=&lon=&ts=1528438782&sign=zREo9KopFt13hDOFqafmK/2kFOAo4Ae06gt1Dt6iGkt48ErR02zJ6/KXOnxX046I&encryption=1&canal=appstore&offset=0&size=10&fn=9
//    https://c.m.163.com/recommend/getChanListNews?channel=T1457068979049&passport=&devId=IIuuXb/hLUSPTntXq4kI%2BoRnb0c8Tfr%2BE7bHyJYx5KpjHUvhlNSkVu2N48qn/bQm&version=37.1&spever=false&net=wifi&lat=&lon=&ts=1528438782&sign=zREo9KopFt13hDOFqafmK/2kFOAo4Ae06gt1Dt6iGkt48ErR02zJ6/KXOnxX046I&encryption=1&canal=appstore&offset=0&size=10&fn=9
//    https://c.m.163.com/recommend/getChanListNews?channel=T1457068979049&passport=&devId=IIuuXb/hLUSPTntXq4kI%2BoRnb0c8Tfr%2BE7bHyJYx5KpjHUvhlNSkVu2N48qn/bQm&version=37.1&spever=false&net=wifi&lat=&lon=&ts=1528438782&sign=zREo9KopFt13hDOFqafmK/2kFOAo4Ae06gt1Dt6iGkt48ErR02zJ6/KXOnxX046I&encryption=1&canal=appstore&offset=0&size=10&fn=9
//    https://c.m.163.com/recommend/getChanListNews?channel=T1457068979049&passport=&devId=IIuuXb/hLUSPTntXq4kI%2BoRnb0c8Tfr%2BE7bHyJYx5KpjHUvhlNSkVu2N48qn/bQm&version=37.1&spever=false&net=wifi&lat=&lon=&ts=1528438782&sign=zREo9KopFt13hDOFqafmK/2kFOAo4Ae06gt1Dt6iGkt48ErR02zJ6/KXOnxX046I&encryption=1&canal=appstore&offset=0&size=10&fn=9
//    https://c.m.163.com/recommend/getChanListNews?channel=T1457068979049&passport=&devId=IIuuXb/hLUSPTntXq4kI%2BoRnb0c8Tfr%2BE7bHyJYx5KpjHUvhlNSkVu2N48qn/bQm&version=37.1&spever=false&net=wifi&lat=&lon=&ts=1528438782&sign=zREo9KopFt13hDOFqafmK/2kFOAo4Ae06gt1Dt6iGkt48ErR02zJ6/KXOnxX046I&encryption=1&canal=appstore&offset=0&size=10&fn=9

}