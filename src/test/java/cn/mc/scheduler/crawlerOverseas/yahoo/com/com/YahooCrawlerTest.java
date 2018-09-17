package cn.mc.scheduler.crawlerOverseas.yahoo.com.com;

import cn.mc.scheduler.SchedulerApplication;
import cn.mc.scheduler.crawlerOverseas.yahoo.com.sport.YahooSportFootballCrawler;
import cn.mc.scheduler.crawlerOverseas.yahoo.com.sport.YahooSportHomeCrawler;
import cn.mc.scheduler.crawlerOverseas.yahoo.com.sport.YahooSportNBACrawler;
import cn.mc.scheduler.crawlerOverseas.yahoo.com.finance.YahooFinanceCrawler;
import cn.mc.scheduler.crawlerOverseas.yahoo.com.sport.YahooSportGolfCrawler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Yahoo crawler test
 *
 * Created by dai on 2018/9/11.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class YahooCrawlerTest {

    @Autowired
    private YahooFinanceCrawler financeCrawler;

    @Autowired
    private YahooSportHomeCrawler sportHomeCrawler;

    @Autowired
    private YahooSportNBACrawler nbaCrawler;

    @Autowired
    private YahooSportFootballCrawler footballCrawler;

    @Autowired
    private YahooSportGolfCrawler golfCrawler;

    @Test
    public void financeTest() {
        financeCrawler.createCrawler().thread(1).run();
    }

    @Test
    public void sportCrawler() {
        sportHomeCrawler.createCrawler().thread(1).run();
    }

    @Test
    public void sportNBACrawler() {
        nbaCrawler.createCrawler().thread(1).run();
    }

    @Test
    public void sportFootballCrawler() {
        footballCrawler.createCrawler().thread(1).run();
    }

    @Test
    public void sportGolfCrawler() {
        golfCrawler.createCrawler().thread(1).run();
    }

}
