package cn.mc.scheduler.jobs.rest;

import cn.mc.core.dataObject.PushInfoDO;
import cn.mc.core.result.Result;
import cn.mc.scheduler.SchedulerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

@Component
public class PushInfoClient {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private SchedulerProperties schedulerProperties;

    private static final String CACHE_CLEAR_NEWS = "/push/timePushInfo";

    public int sendPuhsInfo(PushInfoDO pushInfoDO) {
        String authServerUrl = schedulerProperties.clearCacheAppServer;
        String requestUrl = String.format("%s%s", authServerUrl, CACHE_CLEAR_NEWS);
        ResponseEntity<Result> responseEntity = restTemplate.postForEntity(
                URI.create(requestUrl), pushInfoDO, Result.class);
        Result result=responseEntity.getBody();
        Integer code=result.getCode();
        return code;
    }

}
