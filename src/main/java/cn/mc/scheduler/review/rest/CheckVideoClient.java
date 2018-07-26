package cn.mc.scheduler.review.rest;

import cn.mc.core.constants.CodeConstants;
import cn.mc.core.utils.HttpUtil;
import cn.mc.scheduler.SchedulerProperties;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

/**
 * 认证 client
 *
 * @auther xl
 * @time 2018/6/29 20:59
 */
@Component
public class CheckVideoClient {

    private static Logger logger = LoggerFactory.getLogger(CheckVideoClient.class);

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private SchedulerProperties schedulerProperties;

    private static final String VIDEO_REVIEW_SERVICE_URL = "/grabber/videoReviewSimple";

    public boolean checkVideoUrl(String url) {
        Boolean checkResult = getCheckVideoData(url);
        return checkResult;
    }


    private Boolean getCheckVideoData(String url) {
        String authServerUrl = schedulerProperties.videoImageReviewServer;
        String requestUrl = String.format("%s%s", authServerUrl, VIDEO_REVIEW_SERVICE_URL);
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
