package cn.mc.scheduler.review.rest;

import cn.mc.core.constants.CodeConstants;
import cn.mc.core.dataObject.ReviewFailDO;
import cn.mc.core.mybatis.Update;
import cn.mc.core.result.Result;
import cn.mc.core.utils.HttpUtil;
import cn.mc.scheduler.SchedulerProperties;
import cn.mc.scheduler.mapper.SchedulerLogsMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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

    @Autowired
    private SchedulerLogsMapper schedulerLogsMapper;

    private static final String VIDEO_REVIEW_SERVICE_URL = "/grabber/videoReviewSimple";

    public boolean checkVideoUrl(String url,Long newsId) {
        Result result  = getCheckVideoData(url);
        JSONObject jsonObject = JSON.parseObject(result.getResult().toString());
        Boolean reviewResult=jsonObject.getBoolean("videoReviewResult");
        Double reviewValue=jsonObject.getDoubleValue("reviewResult");
        if(reviewResult==false){
            String  imageUrl=jsonObject.getString("imageUrl");
            ReviewFailDO reviewFailDO=new ReviewFailDO();
            reviewFailDO.setNewsId(newsId);
            reviewFailDO.setImageUrl(imageUrl);
            reviewFailDO.setReviewValue(reviewValue);
            reviewFailDO.setVideoUrl(url);
            schedulerLogsMapper.insertReviewFailLog(Update.copyWithoutNull(reviewFailDO));
        }
        return reviewResult;
    }


    private Result getCheckVideoData(String url) {
        String authServerUrl = schedulerProperties.videoImageReviewServer;
        String requestUrl = String.format("%s%s", authServerUrl, VIDEO_REVIEW_SERVICE_URL);
        try {
            ResponseEntity<Result> responseEntity = restTemplate.postForEntity(
                    URI.create(requestUrl), url, Result.class);
            return responseEntity.getBody();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
