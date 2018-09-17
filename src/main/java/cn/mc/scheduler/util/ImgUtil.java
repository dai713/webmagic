package cn.mc.scheduler.util;

import cn.mc.core.utils.Http2Util;
import cn.mc.core.utils.HttpUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ImgUtil {
    public static InputStream getInputStream(String imgURL) throws IOException {
        imgURL = urlEncodeChinese(imgURL);
        URL u = new URL(imgURL);
        String httpUrl;
        if (imgURL.startsWith("https")) {
            httpUrl = "https://" + u.getHost();
        } else {
            httpUrl = "http://" + u.getHost();
        }
        URLConnection con = u.openConnection();
        con.setReadTimeout(5000);
        con.setRequestProperty("referer", httpUrl); //这是破解防盗链添加的参数
        con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        con.setRequestProperty("Accept-Encoding", "gzip, deflate");
        con.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        con.setRequestProperty("Cache-Control", "no-cache");
        con.setRequestProperty("Host", httpUrl);
        con.setRequestProperty("Pragma", "no-cache");
        con.setRequestProperty("Upgrade-Insecure-Requests", "1");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1");
        con.connect();
        return con.getInputStream();
    }

    public static String urlEncodeChinese(String url) {
        try {
            Matcher matcher = Pattern.compile("[\\u4e00-\\u9fa5]").matcher(url);
            String tmp = "";
            while (matcher.find()) {
                tmp = matcher.group();
                url = url.replaceAll(tmp, URLEncoder.encode(tmp, "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return url;
        }
        return url;
    }
}
