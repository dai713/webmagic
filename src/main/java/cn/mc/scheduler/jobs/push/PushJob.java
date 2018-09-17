package cn.mc.scheduler.jobs.push;

import cn.mc.core.dataObject.PushInfoDO;
import cn.mc.core.exception.SchedulerNewException;
import cn.mc.core.mybatis.Field;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.jobs.rest.PushInfoClient;
import cn.mc.scheduler.mapper.PushMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时推送消息任务
 *
 * @author xl
 * @date 2018-8-22 下午18:17
 */
@Component
public class PushJob extends BaseJob {

    @Autowired
    private PushMapper pushMapper;
    @Autowired
    private PushInfoClient pushInfoClient;

    @Override
    public void execute() throws SchedulerNewException {
        List<PushInfoDO> listPush = pushMapper.selectTimePush(new Field());
        for (PushInfoDO pushInfoDO : listPush) {
            pushInfoClient.sendPuhsInfo(pushInfoDO);
            pushMapper.updatePushStatus(pushInfoDO.getPushId());
        }
    }
}
