package cn.mc.scheduler.base;

import cn.mc.core.dataObject.logs.SchedulerLogsDO;
import cn.mc.core.exception.SchedulerException;
import cn.mc.core.exception.SchedulerNewException;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.BeanManager;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.mapper.SchedulerLogsMapper;
import cn.mc.scheduler.mapper.SchedulerMapper;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.util.Date;

/**
 * 任务调用
 *
 * @auther sin
 * @time 2018/2/2 13:38
 */
public class InvokingProxyJob extends CoreBaseJob {

    @Override
    protected void doProcess(JobExecutionContext context) {
        JobDetail jobDetail = context.getJobDetail();
        String jobName = jobDetail.getKey().getName();
        String jobGroup = jobDetail.getKey().getGroup();
        Long schedulerId = jobDetail.getJobDataMap().getLong("schedulerId");
        String triggerType = jobDetail.getJobDataMap().getString("triggerType");
        String triggerName = jobDetail.getJobDataMap().getString("triggerName");
        String triggerGroup = jobDetail.getJobDataMap().getString("triggerGroup");
        BaseJob job = applicationContext.getBean(jobName, BaseJob.class);

        if (job != null) {
            Long startTime = System.currentTimeMillis();

            // execute
            try {
                job.execute();
            } catch (SchedulerNewException e) {
                throw new RuntimeException(e);
            }

            Long endTime = System.currentTimeMillis();
            Long consumingTime = endTime - startTime;

            // 更新执行时间
            SchedulerMapper schedulerMapper = BeanManager.getBean(SchedulerMapper.class);
            schedulerMapper.updateById(schedulerId,
                    Update.update("startTime", startTime)
                            .set("endTime", endTime)
                            .set("consumingTime", consumingTime)
                            .incr("executeNumber", 1));
            // 记录执行日志
            SchedulerLogsDO schedulerLogsDO = new SchedulerLogsDO();
            schedulerLogsDO.setId(IDUtil.getNewID());
            schedulerLogsDO.setSchedulerId(schedulerId);
            schedulerLogsDO.setStartTime(startTime);
            schedulerLogsDO.setEndTime(endTime);
            schedulerLogsDO.setConsumingTime(consumingTime);

            schedulerLogsDO.setJobName(jobName);
            schedulerLogsDO.setJobGroup(jobGroup);
            schedulerLogsDO.setTriggerType(triggerType);
            schedulerLogsDO.setTriggerName(triggerName);
            schedulerLogsDO.setTriggerGroup(triggerGroup);
            schedulerLogsDO.setAddTime(new Date());

            SchedulerLogsMapper schedulerLogsMapper = BeanManager.getBean(SchedulerLogsMapper.class);
            schedulerLogsMapper.insert(Update.copyWithoutNull(schedulerLogsDO));
        } else {
            throw new RuntimeException("it has not bean name : " + jobName);
        }
    }
}
