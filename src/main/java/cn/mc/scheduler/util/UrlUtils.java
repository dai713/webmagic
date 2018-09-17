package cn.mc.scheduler.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.util.StringUtils;

import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {

    static String[] jsJumps = {
            "window.location.href",
            "location.href",
            "window.top.location",
            "top.location",
            "window.self.location",
            "self.location",
            "window.parent.location.href",
            "parent.location.href"
    };

    public static String buildUrl(String curUrl, String srcUrl) {
        if (isFirstAlt(srcUrl)) {
            return firstAltReplaceUrl(srcUrl, curUrl);
        }
        if ("#".equals(srcUrl)) {
            if (!"/".equals(curUrl.substring(curUrl.length() - 1))) {
                curUrl += "/";
            }
        }
        srcUrl = srcUrl.replaceAll("\\\\", "/");
        if (srcUrl.startsWith("http://") || srcUrl.startsWith("https://") || srcUrl.startsWith("//")
                || srcUrl.startsWith("HTTP://") || srcUrl.startsWith("HTTPS://")) {
            return srcUrl;
        }
        String startsWith = "../";
        if (srcUrl.startsWith(startsWith)) {
            curUrl = checkUrl(curUrl);
            while (srcUrl.startsWith(startsWith)) {
                curUrl = checkUrl(curUrl);
                srcUrl = srcUrl.replaceFirst(startsWith, "");
            }
            return curUrl + "/" + srcUrl;
        } else if(srcUrl.startsWith("/")) {
            curUrl = checkUrl(curUrl);
            curUrl = checkUrl(curUrl);
            return domainUrl(curUrl) + srcUrl;
        } else {
            if (srcUrl.startsWith("./")) {
                srcUrl = srcUrl.replaceFirst("./", "");
            }
            curUrl = checkUrl(curUrl);
            return curUrl + "/" + srcUrl;
        }
    }

    public static boolean isFirstAlt(String url){
        if (url.startsWith("@")) {
            return true;
        } else {
            return false;
        }
    }

    public static String firstAltReplaceUrl(String url, String baseUrl){
        if(isFirstAlt(url)) {
            url = url.replaceFirst("@", domainUrl(baseUrl));
        }
        return url;
    }

    /**
     * 获取主域名地址
     * @param domain
     * @return
     */
    public static String domainUrl(String domain) {
        if (domain.startsWith("http://") || domain.startsWith("HTTP://")) {
            String temp = domain.replaceFirst("http://", "").replaceFirst("HTTP://", "");
            if (temp.indexOf("/") > -1) {
                domain = "http://" + temp.substring(0, temp.indexOf("/"));
            } else {
                return domain;
            }
        } else if (domain.startsWith("https://") || domain.startsWith("HTTPS://")) {
            String temp = domain.replaceFirst("https://", "").replaceFirst("HTTPS://", "");
            if (temp.indexOf("/") > -1) {
                domain = "https://" + temp.substring(0, temp.indexOf("/"));
            } else {
                return domain;
            }
        } else if (domain.startsWith("//")) {
            String temp = domain.replace("//", "");
            if (temp.indexOf("/") > -1) {
                domain = "//" + temp.substring(0, temp.indexOf("/"));
            } else {
                return domain;
            }
        } else {
            if (domain.indexOf("/") > -1) {
                domain = domain.substring(0, domain.indexOf("/"));
            } else {
                return domain;
            }
        }
        return domain;
    }

    public static String checkUrl(String url){
        String profix = url.substring(0, url.lastIndexOf("/"));
        if (!"http:/".equals(profix) && !"https:/".equals(profix) && !"/".equals(profix) && !"HTTP:/".equals(profix) && !"HTTPS:/".equals(profix)) {
            url = url.substring(0, url.lastIndexOf("/"));
        }
        return url;
    }

    public static String urlToUtf8(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0 && c <= 255) {
                if (c == 32) {
                    sb.append("%" + Integer.toHexString(32).toUpperCase());
                } else {
                    sb.append(c);
                }
            } else {
                byte[] b;
                try {
                    b = String.valueOf(c).getBytes("utf-8");
                } catch (Exception ex) {
                    System.out.println(ex);
                    b = new byte[0];
                }
                for (int j = 0; j < b.length; j++) {
                    int k = b[j];
                    if (k < 0)
                        k += 256;
                    sb.append("%" + Integer.toHexString(k).toUpperCase());
                }
            }
        }
        return sb.toString();
    }

    /**
     * 是否存在重定向
     * @param url
     * @param resHtml
     * @return
     */
    public static String isUrlRedirectUrl(HttpURLConnection connection, String url, String resHtml){
        /**
         * meta重定向
         */
        String redirectUrl = metaUrlRedirectUrl(url, resHtml);
        if (!org.springframework.util.StringUtils.isEmpty(redirectUrl)) {
            return redirectUrl;
        }
        /**
         * js重定向
         */
        redirectUrl = jsUrlRedirectUrl(url, resHtml);
        if (!org.springframework.util.StringUtils.isEmpty(redirectUrl)) {
            return redirectUrl;
        }

        /**
         * 302跳转
         */
        redirectUrl = redirectUrl(connection);
        if (!org.springframework.util.StringUtils.isEmpty(redirectUrl)) {
            return redirectUrl;
        }

        /**
         * ajax跳转
         */
        redirectUrl = ajaxUrlRedirectUrl(url, resHtml);
        if (!StringUtils.isEmpty(redirectUrl)) {
            return redirectUrl;
        }
        return redirectUrl;
    }

    public static String redirectUrl(HttpURLConnection connection) {
        String url = "";
        try {
            if (connection.getResponseCode() == 302) {
                String redirectUrl = connection.getHeaderField("Location");
                if(redirectUrl != null && !redirectUrl.isEmpty()) {
                    return redirectUrl;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    /**
     * meta重定向
     * @param url
     * @param resHtml
     * @return
     */
    public static String metaUrlRedirectUrl(String url, String resHtml) {
        String redirectUrl = "";
        Document doc = Jsoup.parse(resHtml);
        Element element = doc.select("meta[HTTP-EQUIV=REFRESH]").first();
        if (element != null) {
            redirectUrl = element.attr("CONTENT");
            String startWith = "URL=";
            if (redirectUrl.indexOf(startWith) > -1) {
                redirectUrl = redirectUrl.substring(redirectUrl.indexOf(startWith), redirectUrl.length());
                redirectUrl = redirectUrl.replaceFirst(startWith, "");
            } else {
                startWith = "url=";
                if (redirectUrl.indexOf(startWith) > -1) {
                    redirectUrl = redirectUrl.substring(redirectUrl.indexOf(startWith), redirectUrl.length());
                    redirectUrl = redirectUrl.replaceFirst(startWith, "");
                }
            }

            if (!org.springframework.util.StringUtils.isEmpty(redirectUrl)) {
                return UrlUtils.buildUrl(url, redirectUrl);
            }
        }
        return redirectUrl;
    }

    /**
     * js重定向
     * @return
     */
    public static String jsUrlRedirectUrl(String url, String resHtml) {
        String redirectUrl = "";
        Document doc = Jsoup.parse(resHtml);
        Elements eles = doc.select("script");

        for (Element element : eles) {
            String jsData = element.data();
            jsData = jsData.trim();
            for (String jump : jsJumps) {
                if (jsData.startsWith(jump)) {
                    redirectUrl = startJsUrl(jsData);
                }
            }
        }
        if (!org.springframework.util.StringUtils.isEmpty(redirectUrl)) {
            return UrlUtils.buildUrl(url, redirectUrl);
        }
        return redirectUrl;
    }

    public static String ajaxUrlRedirectUrl(String url, String html) {
        String redirectUrl = "";
        String regex = "^[a-z|A-Z|0-9]*.html$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            redirectUrl = matcher.group(0);
            break;
        }
        if (url.contains("paper.eznews.cn/ajax")) {
            redirectUrl = "/paperlist/" + redirectUrl;
        }
        if (!StringUtils.isEmpty(redirectUrl)) {
            return UrlUtils.buildUrl(url, redirectUrl);
        }
        return redirectUrl;
    }

    /**
     * 正则表达式获取js跳转地址
     * @param html
     * @return
     */
    public static String startJsUrl(String html){
        String url = "";
        String regex = "^(location.href.*?(\"|\')"
                + "|window.location.href.*?(\"|\')"
                + "|window.top.location.*?(\"|\')"
                + "|top.location.*?(\"|\')"
                + "|window.parent.location.href.*?(\"|\')"
                + "|parent.location.href.*?(\"|\')"
                + "|window.self.location.*?(\"|\')"
                + "|self.location.*?(\"|\')" + ")(.*?)((\"|\'))";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            url = matcher.group(10);
            break;
        }
        return url;
    }

    /**
     * 正则表达式获取js标题跳转地址
     * @param jsFunctionName
     * @param value
     * @return
     */
    public static String titleUrl(String jsFunctionName, String value) {
        String url = "";
        String regex = "(" + jsFunctionName + "\\((\"|\'))(.*?)(\"|\')";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            url = matcher.group(3);
            break;
        }
        return url;
    }

    public static String urlSuffix(String url) {
        if (url.lastIndexOf(".") > -1) {
            return url.substring(url.lastIndexOf("."));
        } else {
            return url;
        }
    }
}
