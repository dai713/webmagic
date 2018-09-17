package cn.mc.scheduler.util;

import cn.mc.scheduler.script.news.CheckNewsJobDataScript;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * 爬虫工具类
 *
 * @author sin
 * @time 2018/7/23 20:16
 */
@Component
public class CrawlerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerUtil.class);

    @Autowired
    public RedisUtil redisUtil;

    /**
     * 获取视频 size
     *
     * @param videoUrl
     * @return
     */
    public static Integer getVideoSize(@NotNull String videoUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(videoUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            return connection.getContentLength();
        } catch (Exception e) {
            // skip
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 设置每个缓存对象，用来监控数据抓取
     *
     * @param key value
     * @return
     */
    public void addNewsTime(String key) {
        String redisKey = CheckNewsJobDataScript.redisKey;
        Map<String, String> map = redisUtil.hmget(redisKey);
        Long time = System.currentTimeMillis();
        map.put(key, time.toString());
        redisUtil.hmset(redisKey, map);
    }
}
