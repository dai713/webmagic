package cn.mc.scheduler.service;

import cn.mc.core.dataObject.SystemKeywordsDO;
import cn.mc.scheduler.SchedulerProperties;
import cn.mc.scheduler.mapper.SystemKeywordsMapper;
import cn.mc.scheduler.util.RedisUtil;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统关键字service
 *
 * @auther daiqingwen
 * @date 2018-8-20 上午10:27
 */

@Service
public class SystemKeywordsService {

    @Autowired
    private SchedulerProperties schedulerProperties;

    @Autowired
    private SystemKeywordsMapper keywordsMapper;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 更新关键字缓存
     */
    public void refreshCache() {
        List<SystemKeywordsDO> jumpList = keywordsMapper.queryNeedJump();
        List<SystemKeywordsDO> replaceList = keywordsMapper.queryNeedReplace();
        redisUtil.remove(schedulerProperties.matched);
        redisUtil.remove(schedulerProperties.replace);
        redisUtil.setString(schedulerProperties.matched, JSON.toJSONString(jumpList));
        redisUtil.setString(schedulerProperties.replace, JSON.toJSONString(replaceList));
    }

}
