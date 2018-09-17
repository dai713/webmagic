package cn.mc.scheduler.crawler.www.alexa.cn;

import cn.mc.scheduler.crawler.BaseCrawler;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 海外 url 抓取
 *
 * @auther sin
 * @time 2018/3/10 16:14
 */
@Component
public class UrlCrawler extends BaseCrawler {

    private Site site = Site.me().setRetryTimes(1).setSleepTime(1);

    private Logger logger = LoggerFactory.getLogger(UrlCrawler.class);

    private static final String URL = "http://www.alexa.cn/siterank/%s";

    @Override
    public Spider createCrawler() {
        List<Request> requestUrls = Lists.newArrayList();
        for (int i = 0; i < 100; i++) {
            String requestUrl = String.format(URL, i + 1);

            Request request = new Request(requestUrl);
            addHeader(request);
            requestUrls.add(request);
        }
        return Spider.create(this)
                .addRequest(requestUrls.toArray(new Request[requestUrls.size()]));
    }

    private void addHeader(Request request) {
        request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        request.addHeader("Accept-Encoding", "gzip, deflate");
        request.addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        request.addHeader("Cache-Control", "no-cache");
        request.addHeader("Connection", "keep-alive");
        request.addHeader("Host", "www.alexa.cn");
        request.addHeader("Upgrade-Insecure-Requests", "1");
        request.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.181 Safari/537.36");
    }


    private static LinkedHashMap<String, String> data = Maps.newLinkedHashMap();

    int index = 0;
    @Override
    public void process(Page page) {
        index ++;
        Html html = page.getHtml();
        List<Selectable> nodes = html.xpath("//div[@class='domain']").nodes();

        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }

        for (Selectable node : nodes) {
            String key = node.xpath("//a/text()").toString().toLowerCase();
            String value = node.xpath("//a/@href").toString();
            data.put(key, value);
        }

        if (index >= 100) {
//            System.out.println(JSON.toJSONString(data));
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
