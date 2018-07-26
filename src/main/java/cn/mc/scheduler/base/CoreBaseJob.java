package cn.mc.scheduler.base;

import cn.mc.core.dataObject.logs.FailLogsDO;
import cn.mc.core.dataObject.scheduler.SchedulerDO;
import cn.mc.core.mail.MailUtil;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.BeanManager;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.mapper.FailLogsMapper;
import cn.mc.scheduler.mapper.SchedulerMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.io.Serializable;
import java.util.Date;

/**
 * @auther sin
 * @time 2018/2/2 13:38
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public abstract class CoreBaseJob extends QuartzJobBean implements Serializable {

    private static final String SUBJECT = "scheduler 异常";

    private static final String[] ERROR_MAIL = new String[]{"cherishsince@aliyun.com", "zhengfa@sh.cool"};

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    protected ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            doProcess(context);
        } catch(DuplicateKeyException e) {
            // skip
            logger.error("此一次不作处理！[executeInternal][异常：{}]", ExceptionUtils.getStackTrace(e));
        } catch (NullPointerException e) {
            // skip
            logger.error("此一次不作处理！[executeInternal][异常：{}]", ExceptionUtils.getStackTrace(e));
            // 添加日志
            addLogs(context, ExceptionUtils.getStackTrace(e));
            // 发送邮件
            MailUtil.sendSystemError(SUBJECT, e.getMessage());
        } catch (Throwable e) {
            // log下异常
            logger.error("[executeInternal][异常：{}]", ExceptionUtils.getStackTrace(e));

            // 添加日志
            addLogs(context, ExceptionUtils.getStackTrace(e));

            // 发送邮件
            MailUtil.sendSystemError(SUBJECT, e.getMessage());

            // 更新失败
            JobDetail jobDetail = context.getJobDetail();
            Long schedulerId = jobDetail.getJobDataMap().getLong("schedulerId");
            SchedulerMapper schedulerMapper = BeanManager.getBean(SchedulerMapper.class);
            schedulerMapper.updateById(schedulerId,
                    Update.update("status", SchedulerDO.STATUS_RUN_FAIL));

            // 抛出异常给上层QUARTZ，并关闭该定时器任务
            JobExecutionException jobExecutionException = new JobExecutionException(e);
            jobExecutionException.setUnscheduleFiringTrigger(true);
            throw jobExecutionException;
        }
    }

    private void addLogs(JobExecutionContext context, String errorMessage) {
        JobDetail jobDetail = context.getJobDetail();
        String jobName = jobDetail.getKey().getName();
        String jobGroup = jobDetail.getKey().getGroup();
        Long schedulerId = jobDetail.getJobDataMap().getLong("schedulerId");

        FailLogsMapper failLogsMapper = BeanManager.getBean(FailLogsMapper.class);
        FailLogsDO failLogsDO = new FailLogsDO();
        failLogsDO.setFailLogsId(IDUtil.getNewID());
        failLogsDO.setParameter(String.format("jobName: %s, " +
                "jobGroup: %s, schedulerId: %s", jobName, jobGroup, schedulerId));
        failLogsDO.setState(FailLogsDO.STATE_UNSOLVED);
        failLogsDO.setSystemContext("");
        failLogsDO.setErrorMessage(errorMessage);
        failLogsDO.setAddTime(new Date());
        failLogsDO.setSystem(FailLogsDO.SYSTEM_SCHEDULER);
        failLogsDO.setLevel(FailLogsDO.LEVEL_SYSTEM);
        failLogsMapper.insert(Update.copyWithoutNull(failLogsDO));
    }

    protected abstract void doProcess(JobExecutionContext context);
}
