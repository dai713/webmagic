package cn.mc.scheduler.jobs.rest;

import cn.mc.core.constants.CodeConstants;
import cn.mc.core.result.Result;
import cn.mc.scheduler.SchedulerProperties;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Component
public class CacheClearClient {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private SchedulerProperties schedulerProperties;

    private static final String CACHE_CLEAR_NEWS = "/cache/clear/news";

    public void sendClearCacheNews(List<CacheClearNewsPO> cacheClearNewsPO) {
        String authServerUrl = schedulerProperties.clearCacheAppServer;
        String requestUrl = String.format("%s%s", authServerUrl, CACHE_CLEAR_NEWS);
        ResponseEntity<Result> responseEntity = restTemplate.postForEntity(
                URI.create(requestUrl), cacheClearNewsPO, Result.class);
        Result result=responseEntity.getBody();
        Integer code=result.getCode();
        //成功
        if(code==0){
            System.out.println("成功");
        }else{
            System.out.println("失败");
        }

    }
}
