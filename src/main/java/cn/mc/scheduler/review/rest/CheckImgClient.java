package cn.mc.scheduler.review.rest;

import cn.mc.core.constants.CodeConstants;
import cn.mc.scheduler.SchedulerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * 图片校验
 *
 * @auther xl
 * @time 2018/6/29 20:59
 */
@Component
public class CheckImgClient {
    private static Logger logger = LoggerFactory.getLogger(CheckImgClient.class);

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private SchedulerProperties schedulerProperties;

    private static final String IMG_REVIEW_SERVICE_URL = "/grabber/imageReviewSimple";

    private boolean checkImgDown(String imgUrl){
        try {
            URL url = new URL(imgUrl);
            // 返回一个 URLConnection 对象，它表示到 URL 所引用的远程对象的连接。
            URLConnection uc = url.openConnection();
            // 打开的连接读取的输入流。
            uc.getInputStream();
        } catch (Exception e) {
             logger.error("图片流对象获取不到返回审核不通过");
             return  false;
        }
        return true;
    }

    public boolean checkImgUrl(String url) {
        logger.info("开始审核图片:" + url);
        Boolean checkResult=checkImgDown(url);
        if(checkResult){
            checkResult = getCheckImgData(url);
        }
        logger.info("审核图片结果:" + checkResult);
        return checkResult;
    }

    private Boolean getCheckImgData(String url) {
        String authServerUrl = schedulerProperties.videoImageReviewServer;
        String requestUrl = String.format("%s%s", authServerUrl, IMG_REVIEW_SERVICE_URL);
        try {
            ResponseEntity<Boolean> responseEntity = restTemplate.postForEntity(
                    URI.create(requestUrl), url, Boolean.class);

            if (CodeConstants.HTTP_SUCCESS_200 != responseEntity.getStatusCode().value()) {
                return false;
            }
            return responseEntity.getBody();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
