package cn.mc.scheduler.util;

import cn.mc.core.utils.EncryptUtil;
import org.jetbrains.annotations.NotNull;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 爬虫工具类
 *
 * @author sin
 * @time 2018/7/23 20:16
 */
public class CrawlerUtil {

    /**
     * 爬虫 - 获取 dataKey
     *
     * @param inputValue
     * @return
     */
    public static String getDataKey(String inputValue) {
        return EncryptUtil.encrypt(inputValue, "md5");
    }

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
}
