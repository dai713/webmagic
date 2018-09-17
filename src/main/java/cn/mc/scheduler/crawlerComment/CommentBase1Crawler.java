package cn.mc.scheduler.crawlerComment;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.scheduler.crawler.CrawlerSupport;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.List;

public abstract class CommentBase1Crawler extends CrawlerSupport implements PageProcessor {
    /**
     * 创建一个爬虫
     *
     *  创建一个 Spider 来进行启动爬虫
     *
     * @return Spider
     */
    public abstract Spider createCrawler(List<NewsDO> newsList);
}
