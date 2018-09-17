package cn.mc.scheduler.util;

import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageUtils {

	public static InputStream downloadImg(String imgUrl, int threadtime){
        InputStream inputStream = null;
        HttpURLConnection connection = null;
        try {
			//new一个URL对象
			URL url = new URL(imgUrl);
			//打开链接
            connection = (HttpURLConnection)url.openConnection();
			//设置请求方式为"GET"
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Referer", imgUrl);
			//超时响应时间为5秒
            connection.setConnectTimeout(30 * 1000);

            return connection.getInputStream();
		} catch (Exception e) {
			try {
                if (connection.getResponseCode() == 503) {
                    Thread.sleep(threadtime);
                } else {
                    e.printStackTrace();
                }
            } catch (Exception ex) {

            }
			System.out.println("图片地址无效：" + imgUrl);
		}
        return inputStream;
	}

    public static String downloadImage(String imgUrl){
        String returnUrl = null;
        try {
            if (StringUtils.isEmpty(imgUrl)) {
                return imgUrl;
            }
            InputStream inputStream = downloadImg(imgUrl, 0);
            if (imgUrl.startsWith("//")) {
                inputStream = ImageUtils.downloadImg("http:"+imgUrl, 0);
                if (inputStream == null) {
                    inputStream = ImageUtils.downloadImg("http:"+imgUrl, 0);
                }
            } else
            {
                inputStream = ImageUtils.downloadImg(imgUrl, 0);
            }
            if (inputStream != null) {
                    /*String fileName = System.currentTimeMillis() + imgUrl.substring(imgUrl.lastIndexOf("."));
                    String fileUrl = FileUtils.uploadFile(fileName, ImageUtils.downloadImg("http:"+imgUrl));
                    if (StringUtils.isEmpty(fileUrl)) {
                        GrabPaperLogDO grabPaperLogDO = new GrabPaperLogDO();
                        grabPaperLogDO.setUrl(imgUrl);
                        grabPaperLogDO.setMessage(imgUrl + "内容图片上传cdn失败");
                        updatePaperByGrapStatus(paperDO.getId(), 5, grabPaperLogDO);//标题地址连接失败
                        return false;
                    }*/

                returnUrl = imgUrl;
//            element.attr("src", fileUrl);
            } else {
                for (int i = 0; i < 3; i++) {
                    System.out.println("===========图片下载失败，重新尝试第" + (i + 1)  + "次");
                    inputStream = downloadImg(imgUrl, (i + 1) * 1000);
                    if (inputStream != null) {
                        System.out.println("===========图片重新尝试下载成功");
                        returnUrl = imgUrl;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnUrl;
    }

    public static void main(String[] s) {
        String imgUrl = downloadImage("http://epaper.wxrb.com/paper/wxrb/resfiles/2018-08/06/s_713933_365402.jpg");
        System.out.println("imgUrl:" + imgUrl);
    }
}
