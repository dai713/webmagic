package cn.mc.scheduler.crawler;

import cn.mc.scheduler.SchedulerApplication;
import cn.mc.scheduler.crawler.engadget.com.EngadgetCrawlers;
import cn.mc.scheduler.crawler.feng.com.WeiFengCrawlers;
import cn.mc.scheduler.crawler.geekpark.com.GeekParkCrawlers;
import cn.mc.scheduler.crawler.huxiu.com.HuXiuCrawlers;
import cn.mc.scheduler.crawler.ifanr.com.IfanrCrawlers;
import cn.mc.scheduler.crawler.lanxiongsports.com.LanXiongSportsCrawlers;
import cn.mc.scheduler.crawler.news.baidu.com.BaiDuEntertainmentCrawler;
import cn.mc.scheduler.crawler.news.baidu.com.SearchNewsCrawler;
import cn.mc.scheduler.crawler.sports.eastday.com.EastSportsCrawlers;
import cn.mc.scheduler.crawler.weixin.sogou.com.WeiXinSoGouCrawler;
import cn.mc.scheduler.util.AliyunOSSClientUtil;
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
    @Autowired
    private LanXiongSportsCrawlers lanXiongSportsCrawlers;
    @Autowired
    private EastSportsCrawlers eastSportsCrawlers;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;

    @Test
    public void baiDuEntertainmentCrawlerTest() {
        baiDuEntertainmentCrawler.createCrawler().thread(1).run();
    }

    @Test
    public void startSearchNewsCrawlerTest() {
        searchNewsCrawler.createCrawler().thread(1).run();
    }

    @Test
    public void geekParkCrawlers() {
        geekParkCrawlers.createCrawler().thread(1).run();
    }

    @Test
    public void weiXinSoGouCrawlersTest() {
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

    @Test
    public void lanXiongCrawlers() {
        lanXiongSportsCrawlers.createCrawler().thread(1).run();
    }

    @Test
    public void eastCrawlers() {
        eastSportsCrawlers.createCrawler().thread(1).run();
    }

    @Test
    public void upload() {
//        String imgUrl1 = "file:///C:\\Users\\cp110\\Desktop\\test\\大图.png";
//        String imgUrl2 = "file:///C:\\Users\\cp110\\Desktop\\test\\单图1.png";
//        String imgUrl3 = "file:///C:\\Users\\cp110\\Desktop\\test\\三图2.png";
//        String imgUrl4 = "file:///C:\\Users\\cp110\\Desktop\\test\\三图3.png";
//        String imgUrl5 = "file:///C:\\Users\\cp110\\Desktop\\test\\三图1.png";
        String imgUrl6 = "file:///C:\\Users\\cp110\\Desktop\\test\\1125x2076.png";
//        String saveImg0 = aliyunOSSClientUtil.uploadPicture(imgUrl1);
//        String saveImg1 = aliyunOSSClientUtil.uploadPicture(imgUrl2);
//        String saveImg2 = aliyunOSSClientUtil.uploadPicture(imgUrl3);
//        String saveImg3 = aliyunOSSClientUtil.uploadPicture(imgUrl4);
//        String saveImg4 = aliyunOSSClientUtil.uploadPicture(imgUrl5);
        String saveImg5 = aliyunOSSClientUtil.uploadPicture(imgUrl6);
//        System.out.println("图片地址为：" + saveImg0);
//        System.out.println("图片地址为：" + saveImg1);
//        System.out.println("图片地址为：" + saveImg2);
//        System.out.println("图片地址为：" + saveImg3);
//        System.out.println("图片地址为：" + saveImg4);
        System.out.println("图片地址为：" + saveImg5);
    }
}
