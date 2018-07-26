package cn.mc.scheduler.crawler.news.baidu.com;

import cn.mc.scheduler.SchedulerApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 百度 - 体育视频 test
 *
 * @author sin
 * @time 2018/7/23 20:23
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class BaiDuSportsVideoCrawlerTest {

    @Autowired
    private BaiDuSportsVideoCrawler baiDuSportsVideoCrawler;

    @Test
    public void baiDuSportsVideoCrawlerTest() {
        baiDuSportsVideoCrawler.createCrawler().thread(2).run();
    }
}
