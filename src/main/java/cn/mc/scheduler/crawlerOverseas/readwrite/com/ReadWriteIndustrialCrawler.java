package cn.mc.scheduler.crawlerOverseas.readwrite.com;

import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

/**
 * readwrite.com - 产业 爬虫
 *
 * @author Sin
 * @time 2018/8/28 下午8:11
 */
@Component
public class ReadWriteIndustrialCrawler extends BaseCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadWriteIndustrialCrawler.class);
    private static final String URL = "http://readwrite.com/category/industrial/";
    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(300);

    @Autowired
    private ReadWriteAiCrawler readWriteAiCrawler;

    @Override
    public Spider createCrawler() {
        Request request = new Request(URL);
        readWriteAiCrawler.addHeader(request);
        return new BaseSpider(this).addRequest(request);
    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();
        if (url.equals(URL)) {
            readWriteAiCrawler.handleList(page);
        } else {
            readWriteAiCrawler.handleDetail(page);
        }
    }

    @Override
    public Site getSite() {
        return SITE;
    }
}
