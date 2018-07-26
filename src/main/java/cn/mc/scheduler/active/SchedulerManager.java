package cn.mc.scheduler.active;

import cn.mc.core.dataObject.scheduler.SchedulerDO;
import cn.mc.core.entity.Page;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.BeanManager;
import cn.mc.scheduler.active.po.SaveSchedulerPO;
import cn.mc.scheduler.active.query.ScheduleListQuery;
import cn.mc.scheduler.mapper.SchedulerMapper;
import org.quartz.CronScheduleBuilder;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * @auther sin
 * @time 2018/2/3 14:30
 */
@Service
public class SchedulerManager {

    @Autowired
    private SchedulerMapper schedulerMapper;

    /**
     * 定时器 - 列表
     *
     * @param index 分页
     * @param scheduleListQuery
     * @return scheduler 列表.
     */
    public Page selectList(Integer index,
                           ScheduleListQuery scheduleListQuery) {
        Page page = new Page<>();
        page.setIndex(index);

        List<SchedulerDO> schedulerDOList = schedulerMapper
                .selectList(scheduleListQuery, page, new Field());

        if (!CollectionUtils.isEmpty(schedulerDOList)) {
            Long totalCount = schedulerMapper.selectListCount(scheduleListQuery, page);
            page.setTotal(totalCount);
        }

        page.setData(schedulerDOList);
        return page;
    }

    /**
     * 定时器 - 插入一个
     *
     *  <p>
     *      并设置 scheduler 默认信息
     *  </p>
     *
     * @param schedulerId 定时器 id
     * @param saveSchedulerPO 需要保存的 po
     * @return
     */
    public int insertScheduler(Long schedulerId, SaveSchedulerPO saveSchedulerPO) {

        // 保存定时器任务
        SchedulerDO schedulerDO = new SchedulerDO();
        schedulerDO.setId(schedulerId);
        schedulerDO.setJobName(saveSchedulerPO.getJobName());
        schedulerDO.setJobGroup(saveSchedulerPO.getJobGroup());

        // TODO 这个名字需要动态获取，不能直接写入
        schedulerDO.setSchedName("browserSchedulerFactoryBean");
        schedulerDO.setStartTime(0L);
        schedulerDO.setEndTime(0L);
        schedulerDO.setConsumingTime(0L);
        schedulerDO.setExecuteNumber(0L);
        schedulerDO.setDescription(saveSchedulerPO.getDescription());
        schedulerDO.setTriggerName(saveSchedulerPO.getTriggerName());
        schedulerDO.setTriggerType(saveSchedulerPO.getTriggerType());
        schedulerDO.setTriggerGroup(saveSchedulerPO.getTriggerGroup());
        schedulerDO.setTriggerValue(saveSchedulerPO.getTriggerExpression());
        schedulerDO.setAddTime(new Date());
        schedulerDO.setStatus(SchedulerDO.STATUS_ACQUIRED);

        return schedulerMapper.insert(Update.copyWithoutNull(schedulerDO));
    }

    /**
     * Quartz 触发器 - 创建
     *
     * @param triggerType 触发器类型
     * @param triggerName 触发器名称
     * @param triggerGroup 触发器组
     * @param triggerExpression 触发器表达式、触发器简单表达式
     * @return Quartz 触发器（Trigger）
     */
    public Trigger createTrigger(String triggerType, String triggerName,
                                 String triggerGroup, String triggerExpression) {

        Trigger trigger = null;
        switch (triggerType) {
            case "Simple": {
                trigger = TriggerBuilder.newTrigger()
                        .withIdentity(triggerName, triggerGroup)
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInSeconds(Integer.valueOf(triggerExpression))
                                .repeatForever())
                        .build();
            }
            break;
            case "Cron": {
                trigger = TriggerBuilder.newTrigger()
                        .withIdentity(triggerName, triggerGroup)
                        .withSchedule(CronScheduleBuilder.cronSchedule(triggerExpression))
                        .build();
            }
            break;
        }

        return trigger;
    }

    public boolean checkJobExistence(String jobName, String jobGroup) {
        SchedulerDO schedulerDO = schedulerMapper
                .selectByJobNameAndJobGroup(jobName, jobGroup, new Field());
        return schedulerDO == null ? false : true;
    }

    public int updateRunFailBySchedulerId(Long schedulerId) {
        return schedulerMapper.updateById(schedulerId,
                Update.update("status", SchedulerDO.STATUS_RUN_FAIL));
    }

    public int updateRunFailByJobNameAndJobGroup(String jobName, String jobGroup) {
        return schedulerMapper.updateByJobNameAndJobGroup(jobName, jobGroup,
                Update.update("status", SchedulerDO.STATUS_RUN_FAIL));
    }
}
