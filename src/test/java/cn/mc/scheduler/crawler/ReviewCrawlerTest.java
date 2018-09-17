package cn.mc.scheduler.crawler;

import cn.mc.scheduler.SchedulerApplication;
import cn.mc.scheduler.crawler.ithome.com.ITHomeNewsCrawler;
import cn.mc.scheduler.crawler.news.baidu.com.SearchNewsCrawler;
import cn.mc.scheduler.crawler.sina.cn.SinaNewsCrawler;
import cn.mc.scheduler.crawler.sina.cn.SinaSportCrawler;
import cn.mc.scheduler.crawler.toutiao.com.EntertainmentCrawler2;
import cn.mc.scheduler.crawler.toutiao.com.VideoCrawler;
import cn.mc.scheduler.crawler.wangyi.com.WangYiVideoCrawler;
import cn.mc.scheduler.crawler.wx.qq.com.QQSportNewsCrawler;
import cn.mc.scheduler.crawler.wx.qq.com.QQTechnologyNewsCrawler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author sin
 * @time 2018/7/16 17:10
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class ReviewCrawlerTest {

    @Autowired
    private SearchNewsCrawler searchNewsCrawler;
    @Autowired
    private ITHomeNewsCrawler itHomeNewsCrawler;
    @Autowired
    private EntertainmentCrawler2 entertainmentCrawler2;
    @Autowired
    private SinaSportCrawler sinaSportCrawler;
    @Autowired
    private SinaNewsCrawler sinaNewsCrawler;
    @Autowired
    private QQSportNewsCrawler qqSportNewsCrawler;
    @Autowired
    private QQTechnologyNewsCrawler qqTechnologyNewsCrawler;
    @Autowired
    private VideoCrawler videoCrawler;

    @Autowired
    private WangYiVideoCrawler wangyiVideoCrawlers;

    /// 视频

    @Test
    public void touTiaoVideoTest() {
        for (int i = 0; i < 100; i++) {
            videoCrawler.createCrawler().thread(1).run();
        }
    }


    ///
    /// 新闻

    @Test
    public void searchNewsCrawlerTest1() {
        searchNewsCrawler.createCrawler().thread(1).run();
//        for (int i = 0; i < 100; i++) {
//        }
    }


    @Test
    public void searchNewsCrawlerTest2() {
        for (int i = 0; i < 100; i++) {
            itHomeNewsCrawler.createCrawler().thread(1).run();
        }
    }


    @Test
    public void searchNewsCrawlerTest3() {
        for (int i = 0; i < 100; i++) {
            entertainmentCrawler2.createCrawler().thread(1).run();
        }
    }


    @Test
    public void searchNewsCrawlerTest4() {
        for (int i = 0; i < 100; i++) {
            sinaSportCrawler.createCrawler().thread(1).run();
        }
    }


    @Test
    public void searchNewsCrawlerTest5() {
        for (int i = 0; i < 100; i++) {
            sinaNewsCrawler.createCrawler().thread(1).run();
        }
    }


    @Test
    public void searchNewsCrawlerTest6() {
        for (int i = 0; i < 100; i++) {
            qqSportNewsCrawler.createCrawler().thread(1).run();
        }
    }


    @Test
    public void searchNewsCrawlerTest7() {
        for (int i = 0; i < 100; i++) {
            qqTechnologyNewsCrawler.createCrawler().thread(1).run();
        }
    }
}
