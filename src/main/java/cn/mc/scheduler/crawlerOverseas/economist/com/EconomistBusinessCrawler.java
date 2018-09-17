package cn.mc.scheduler.crawlerOverseas.economist.com;

import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.crawlerOverseas.readwrite.com.ReadWriteAiCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

/**
 * www.economist.com 海外财经 - 商业
 *
 * @author Sin
 * @time 2018/9/14 下午2:13
 */
@Component
public class EconomistBusinessCrawler extends BaseCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadWriteAiCrawler.class);
    private static final String URL = "https://www.economist.com/business/";
    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(300);

    @Autowired
    private EconomistNewestCrawler economistNewestCrawler;

    @Override
    public Spider createCrawler() {
        Request request = new Request(URL);
        economistNewestCrawler.addHeader(request);
        return new BaseSpider(this).addRequest(request);
    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();
        if (url.equals(URL)) {
            economistNewestCrawler.handleList(page);
        } else {
            economistNewestCrawler.handleDetail(page);
        }
    }

    @Override
    public Site getSite() {
        return SITE;
    }
}
