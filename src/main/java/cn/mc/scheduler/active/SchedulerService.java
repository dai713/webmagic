package cn.mc.scheduler.active;

import cn.mc.core.dataObject.scheduler.SchedulerDO;
import cn.mc.core.entity.Page;
import cn.mc.core.exception.SchedulerNewException;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.BeanManager;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.active.po.ResumeSchedulerPO;
import cn.mc.scheduler.active.po.RunSchedulerPO;
import cn.mc.scheduler.active.po.SaveSchedulerPO;
import cn.mc.scheduler.active.po.UpdateSchedulerPO;
import cn.mc.scheduler.active.query.ScheduleListQuery;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.base.InvokingProxyJob;
import cn.mc.scheduler.exception.SchedulerExistenceException;
import cn.mc.scheduler.mapper.SchedulerMapper;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @auther sin
 * @time 2018/2/3 14:21
 */
@Service
public class SchedulerService {

    @Autowired
    private Scheduler scheduler;
    @Autowired
    private SchedulerManager schedulerManager;

    /**
     * 定时器 - 列表
     *
     * @param index 分页 index
     * @param scheduleListQuery
     */
    public Page listScheduler(Integer index,
                              ScheduleListQuery scheduleListQuery) {
        return schedulerManager.selectList(index, scheduleListQuery);
    }

    /**
     * 定时器 - 保存
     *
     * @param saveSchedulerPO 需要保存的信息
     */
    public void saveScheduler(SaveSchedulerPO saveSchedulerPO) {
        String triggerType = saveSchedulerPO.getTriggerType();
        String triggerName = saveSchedulerPO.getTriggerName();
        String triggerGroup = saveSchedulerPO.getTriggerGroup();
        Long schedulerId = IDUtil.getNewID();
        String jobName = saveSchedulerPO.getJobName();
        String jobGroup = saveSchedulerPO.getJobGroup();
        String triggerExpression = saveSchedulerPO.getTriggerExpression();
        String description = saveSchedulerPO.getDescription();

        JobDetail jobDetail = JobBuilder.newJob(InvokingProxyJob.class)
                .withDescription(description)
                .withIdentity(jobName, jobGroup)
                .usingJobData("schedulerId", schedulerId)
                .usingJobData("triggerType", triggerType)
                .usingJobData("triggerName", triggerName)
                .usingJobData("triggerGroup", triggerGroup)
                .build();

        try {
            Trigger trigger = schedulerManager.createTrigger(triggerType,
                    triggerName, triggerGroup, triggerExpression);

            if (schedulerManager.checkJobExistence(jobName, jobGroup)) {
                throw new SchedulerExistenceException("job is existence.");
            }
            scheduler.scheduleJob(jobDetail, trigger);
            schedulerManager.insertScheduler(schedulerId, saveSchedulerPO);
        } catch (Exception e) {
            schedulerManager.updateRunFailBySchedulerId(schedulerId);
            throw new RuntimeException(e);
        }
    }

    /**
     * 定时器 - 更新
     *
     * @param updateSchedulerPO 需要更新的信息
     */
    public void updateScheduler(UpdateSchedulerPO updateSchedulerPO) {

        String triggerType = updateSchedulerPO.getTriggerType();
        String triggerName = updateSchedulerPO.getTriggerName();
        String triggerGroup = updateSchedulerPO.getTriggerGroup();
        String triggerExpression = updateSchedulerPO.getTriggerExpression();
        SchedulerMapper schedulerMapper = BeanManager.getBean(SchedulerMapper.class);

        try {
            Trigger trigger = schedulerManager.createTrigger(triggerType,
                    triggerName, triggerGroup, triggerExpression);

            schedulerMapper.updateById(updateSchedulerPO.getSchedulerId(),
                    Update.update("triggerValue", triggerExpression)
                            .set("triggerType", triggerType)
                            .set("triggerGroup", triggerGroup)
                            .set("triggerName", triggerName)
                            .set("description", updateSchedulerPO.getDescription()));

            scheduler.rescheduleJob(new TriggerKey(triggerName, triggerGroup), trigger);
        } catch (Exception e) {
            schedulerManager.updateRunFailBySchedulerId(updateSchedulerPO.getSchedulerId());
            throw new RuntimeException(e);
        }
    }

    /**
     * 定时器 - 删除
     *
     * @param jobName 任务名
     * @param jobGroup 任务 group
     */
    public void removeScheduler(String jobName, String jobGroup) {
        try {
            SchedulerMapper schedulerMapper = BeanManager.getBean(SchedulerMapper.class);
            schedulerMapper.updateByJobNameAndJobGroup(jobName, jobGroup,
                    Update.update("status", SchedulerDO.STATUS_REMOVE));
            scheduler.deleteJob(new JobKey(jobName, jobGroup));
        } catch (Exception e) {
            schedulerManager.updateRunFailByJobNameAndJobGroup(jobName, jobGroup);
            throw new RuntimeException(e);
        }
    }

    /**
     * 定时器 - 暂停
     *
     * @param jobName 定时器名
     * @param jobGroup 定时器 group
     */
    public void pauseScheduler(String jobName, String jobGroup) {
        try {
            SchedulerMapper schedulerMapper = BeanManager.getBean(SchedulerMapper.class);
            schedulerMapper.updateByJobNameAndJobGroup(jobName, jobGroup,
                    Update.update("status", SchedulerDO.STATUS_PAUSED));

            scheduler.pauseJob(new JobKey(jobName, jobGroup));
        } catch (Exception e) {
            schedulerManager.updateRunFailByJobNameAndJobGroup(jobName, jobGroup);
            throw new RuntimeException(e);
        }
    }

    /**
     * 定时器 - 重启
     *
     * @param jobName 任务名
     * @param jobGroup 任务 group
     * @param resumeSchedulerPO 重启需要的信息
     */
    public void resumeScheduler(String jobName, String jobGroup,
                                ResumeSchedulerPO resumeSchedulerPO) {
        try {
            String triggerName = resumeSchedulerPO.getTriggerName();
            String triggerGroup = resumeSchedulerPO.getTriggerGroup();

            SchedulerMapper schedulerMapper = BeanManager.getBean(SchedulerMapper.class);
            schedulerMapper.updateByJobNameAndJobGroup(jobName, jobGroup,
                    Update.update("status", SchedulerDO.STATUS_ACQUIRED));

            scheduler.resumeJob(new JobKey(jobName, jobGroup));
            scheduler.resumeTrigger(new TriggerKey(triggerName, triggerGroup));
        } catch (Exception e) {
            schedulerManager.updateRunFailByJobNameAndJobGroup(jobName, jobGroup);
            throw new RuntimeException(e);
        }
    }

    /**
     * 定时器 - script 一次
     *
     * <p>
     *     定时器 script 分为 “两种”：
     *
     *      1、调用定时器 script，会记录运行调用的 log
     *
     *      2、BeanManager 调用，采用 spring application-context 注入调用。
     * </p>
     *
     * @param jobName
     * @param runSchedulerPO
     * @return
     */
    public void runScheduler(String jobName, RunSchedulerPO runSchedulerPO)
            throws SchedulerNewException {
        try {
            Integer runType = runSchedulerPO.getRunType();
            String jobGroup = runSchedulerPO.getJobGroup();

            if (RunSchedulerPO.RUN_TYPE_JOB_DETAIL.equals(runType)) {
                scheduler.triggerJob(new JobKey(jobName, jobGroup));
            } else {
                BaseJob baseJob = BeanManager.getBean(jobName);
                baseJob.execute();
            }
        } catch (SchedulerNewException e) {
            schedulerManager.updateRunFailByJobNameAndJobGroup(jobName, runSchedulerPO.getJobGroup());
            throw new SchedulerNewException(e.getMessage()) {
                @Override
                public int getCode() {
                    return e.getCode();
                }
            };
        } catch (Exception e) {
            schedulerManager.updateRunFailByJobNameAndJobGroup(jobName, runSchedulerPO.getJobGroup());
            throw new RuntimeException(e);
        }
    }
}
