package cn.mc.scheduler.util;

import cn.mc.core.utils.Http2Util;
import cn.mc.core.utils.HttpUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;


public class ImgUtil {
    public  synchronized  static  InputStream getInputStream(String imgURL) {
        try {
            URL u = new URL(imgURL);
            URLConnection con = u.openConnection();
            HttpURLConnection httpUrlConnection = (HttpURLConnection) con;
            httpUrlConnection.setRequestProperty("referer", u.getHost()); //这是破解防盗链添加的参数
            httpUrlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            httpUrlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
            httpUrlConnection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
            httpUrlConnection.setRequestProperty("Host", u.getHost());
            httpUrlConnection.setRequestProperty("Pragma", "no-cache");
            httpUrlConnection.setRequestProperty("Upgrade-Insecure-Requests", "1");
            httpUrlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1");
            httpUrlConnection.connect();
            return httpUrlConnection.getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
