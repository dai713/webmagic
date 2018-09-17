package cn.mc.scheduler.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HttpUtils {

	/**
     * 向指定 URL 发送POST方法的请求
     * @param url 发送请求的 URL
     * @param params 请求参数，请求参数应该是 name1=value1&name2=value2 的形式
     * @return
     */
	public static String sendPost(String url, String params){
		PrintWriter out = null;
		BufferedReader in = null;
		String result = "";
		try {
			URL realUrl = new URL(url);
			//打开和URL之间的连接
			URLConnection conn = realUrl.openConnection();
			//设置通用的请求属性
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			//发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			//获取URLConnection对象对应的输出流
			out = new PrintWriter(conn.getOutputStream());
			//发送请求参数
			out.print(params);
			//flush输出流的缓冲
			out.flush();
			//定义BufferedReader输入流来读取URL的响应
			in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while((line = in.readLine()) != null){
				result += line;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				if(out != null){
					out.close();
				}
				if(in != null){
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

    /**
     * 向指定URL发送GET方法的请求
     * @param url 发送请求的URL
     * @param params 请求参数，请求参数应该是 name1=value1&name2=value2 的形式
     * @return
     */
    public static String sendGet(String url, String params, String encoding){
        String result = "";
        BufferedReader in = null;
        HttpURLConnection connection = null;
        try {
            String urlNameString = url;
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(params)) {
                urlNameString += "?" + params;
            }
            URL realUrl = new URL(urlNameString);
            //打开和URL之间的连接
            connection = (HttpURLConnection)realUrl.openConnection();
            //设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            //建立实际的连接
            connection.connect();
            if (connection.getResponseCode() == 302) {
                String redirectUrl = connection.getHeaderField("Location");
                if(redirectUrl != null && !redirectUrl.isEmpty()) {
                    url = redirectUrl;
                    return sendGet(url, params, encoding);
                }
            }
            //定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding));
            String line;
            while((line = in.readLine()) != null){
                result += line;
            }
        } catch (Exception e) {

            try {
                if (connection.getResponseCode() == 503) {
                    Thread.sleep(2000);
                } else {
                    e.printStackTrace();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return "";
        }finally{
            try {
                if(in != null){
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 向指定URL发送GET方法的请求
     * @param url 发送请求的URL
     * @param params 请求参数，请求参数应该是 name1=value1&name2=value2 的形式
     * @return
     */
    public static String sendGet(String url, String params){
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url;
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(params)) {
                urlNameString += "?" + params;
            }
            URL realUrl = new URL(urlNameString);
            //打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection)realUrl.openConnection();
            //设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            //建立实际的连接
            connection.connect();

            //定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            String line;
            while((line = in.readLine()) != null){
                result += line;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }finally{
            try {
                if(in != null){
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 向指定URL发送GET方法的请求
     * @param url 发送请求的URL
     * @param params 请求参数，请求参数应该是 name1=value1&name2=value2 的形式
     * @return
     */
    public static String[] sendGets(String url, String params, String encoding){
        String results[] = {"", ""};
        BufferedReader in = null;
        String result = "";
        HttpURLConnection connection = null;
        try {
            String urlNameString = url;
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(params)) {
                urlNameString += "?" + params;
            }
            URL realUrl = new URL(urlNameString);
            //打开和URL之间的连接
            connection = (HttpURLConnection)realUrl.openConnection();
            //设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            //建立实际的连接
            connection.connect();

            //定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding));
            String line;
            while((line = in.readLine()) != null){
                result += line;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return results;
        }finally{
            try {
                if(in != null){
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        results[0] = url;
        results[1] = result;
        String redirectUrl = UrlUtils.isUrlRedirectUrl(connection, url, result);
        if (!org.springframework.util.StringUtils.isEmpty(redirectUrl)) {
            results = sendGets(redirectUrl, params, encoding);
        } else {
            redirectUrl = url;
            results[0] = redirectUrl;
        }
        return results;
    }


}
