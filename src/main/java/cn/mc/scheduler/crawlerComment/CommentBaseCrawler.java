package cn.mc.scheduler.crawlerComment;

import cn.mc.core.utils.EncryptUtil;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.crawler.CrawlerSupport;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

/**
 * 爬虫 base
 *
 * @auther sin
 * @time 2018/3/7 13:34
 */
public abstract class CommentBaseCrawler extends CrawlerSupport implements PageProcessor {

    /**
     * 创建一个爬虫
     *
     *  创建一个 Spider 来进行启动爬虫
     *
     * @return Spider
     */
    public abstract  Spider createCrawler(String pushId,String product,Long newsId);

}
