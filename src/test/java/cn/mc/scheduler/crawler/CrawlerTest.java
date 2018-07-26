package cn.mc.scheduler.crawler;

import cn.mc.scheduler.SchedulerApplication;
import cn.mc.scheduler.crawler.engadget.com.EngadgetCrawlers;
import cn.mc.scheduler.crawler.feng.com.WeiFengCrawlers;
import cn.mc.scheduler.crawler.geekpark.com.GeekParkCrawlers;
import cn.mc.scheduler.crawler.huxiu.com.HuXiuCrawlers;
import cn.mc.scheduler.crawler.ifanr.com.IfanrCrawlers;
import cn.mc.scheduler.crawler.news.baidu.com.BaiDuEntertainmentCrawler;
import cn.mc.scheduler.crawler.news.baidu.com.SearchNewsCrawler;
import cn.mc.scheduler.crawler.qiushibaike.com.QiuShiCrawler;
import cn.mc.scheduler.crawler.toutiao.com.EntertainmentCrawler;
import cn.mc.scheduler.crawler.weixin.sogou.com.WeiXinSoGouCrawler;
import cn.mc.scheduler.crawler.wukong.com.HotCrawler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @auther sin
 * @time 2018/3/13 20:37
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class CrawlerTest {

    @Autowired
    private SearchNewsCrawler searchNewsCrawler;
    @Autowired
    private BaiDuEntertainmentCrawler baiDuEntertainmentCrawler;
    @Autowired
    private QiuShiCrawler qiuShiCrawler;
    @Autowired
    private EntertainmentCrawler entertainmentCrawler;
    @Autowired
    private HotCrawler hotCrawler;
    @Autowired
    private GeekParkCrawlers geekParkCrawlers;
    @Autowired
    private WeiXinSoGouCrawler weiXinSoGouCrawlers;
    @Autowired
    private WeiFengCrawlers weiFengCrawlers;
    @Autowired
    private HuXiuCrawlers huXiuCrawlers;
    @Autowired
    private IfanrCrawlers ifanrCrawlers;
    @Autowired
    private EngadgetCrawlers engadgetCrawlers;

    @Test
    public void baiDuEntertainmentCrawlerTest() {
        baiDuEntertainmentCrawler.createCrawler().thread(1).run();
    }

    @Test
    public void startSearchNewsCrawlerTest() {
        searchNewsCrawler.createCrawler().thread(1).run();
    }

    @Test
    public void startQiuShiCrawler() {
        qiuShiCrawler.createCrawler().thread(1).run();
    }

    @Test
    public void startEntertainmentCrawler() {
        entertainmentCrawler.createCrawler().thread(1).run();
    }

    @Test
    public void startHotCrawler() {
        hotCrawler.createCrawler().thread(1).run();
    }

    @Test
    public void geekParkCrawlers() {
        geekParkCrawlers.createCrawler().thread(1).run();
    }

    @Test
    public void weinXinCrawlers() {
        weiXinSoGouCrawlers.createCrawler().thread(1).run();
    }

    @Test
    public void weiFengCrawlers() {
        weiFengCrawlers.createCrawler().thread(1).run();
    }

    @Test
    public void HuXiuCrawlers() {
        huXiuCrawlers.createCrawler().thread(1).run();
    }

    @Test
    public void ifanrCrawlers() {
        ifanrCrawlers.createCrawler().thread(1).run();
    }

    @Test
    public void engadgetCrawlers() {
        engadgetCrawlers.createCrawler().thread(1).run();
    }


}
