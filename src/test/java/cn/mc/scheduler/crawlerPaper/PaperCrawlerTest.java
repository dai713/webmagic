package cn.mc.scheduler.crawlerPaper;

import cn.mc.scheduler.SchedulerApplication;
import cn.mc.scheduler.util.HtmlNodeUtil;
import org.jsoup.nodes.Element;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Selectable;

import java.util.List;

/**
 * 报纸 爬虫测试
 *
 * @author Sin
 * @time 2018/9/4 下午7:28
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class PaperCrawlerTest {

    @Autowired
    private PaperCrawler paperCrawler;

    @Test
    public void paperCrawlerTest() {
        paperCrawler.exec();
    }
}
