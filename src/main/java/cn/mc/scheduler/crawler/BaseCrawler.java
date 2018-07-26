package cn.mc.scheduler.crawler;

import cn.mc.core.utils.EncryptUtil;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

/**
 * 爬虫 base
 *
 * @auther sin
 * @time 2018/3/7 13:34
 */
public abstract class BaseCrawler extends CrawlerSupport implements PageProcessor {

    /**
     * 创建一个爬虫
     *
     *  创建一个 Spider 来进行启动爬虫
     *
     * @return Spider
     */
    public abstract Spider createCrawler();

    /**
     * encrypt 一个 value，用于 data key 的生成
     *
     * @param text
     * @return
     */
    protected String encrypt(String text) {
        return EncryptUtil.encrypt(text, "md5");
    }
}
