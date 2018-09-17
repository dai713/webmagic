package cn.mc.scheduler.base;

import cn.mc.core.dataObject.logs.FailLogsDO;
import cn.mc.core.dataObject.scheduler.SchedulerDO;
import cn.mc.core.mail.MailUtil;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.BeanManager;
import cn.mc.core.utils.IDUtil;
import cn.mc.core.utils.MessageUtil;
import cn.mc.scheduler.mapper.FailLogsMapper;
import cn.mc.scheduler.mapper.SchedulerMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Date;

/**
 * core baseJob
 *
 * @auther sin
 * @time 2018/2/2 13:38
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public abstract class CoreBaseJob extends QuartzJobBean implements Serializable {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String SUBJECT = "scheduler 异常";
    private static final String MAIL_SUBJECT_TEMPLATE = "{active} {subject}";

    protected ApplicationContext applicationContext;

    protected Environment environment;

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.environment = applicationContext.getBean(Environment.class);

        Assert.notNull(environment, "系统参数 environment 没有注入!");
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
            sendErrorMail(e);
        } catch (Throwable e) {
            // log下异常
            logger.error("[executeInternal][异常：{}]", ExceptionUtils.getStackTrace(e));

            // 添加日志
            addLogs(context, ExceptionUtils.getStackTrace(e));

            // 发送邮件
            sendErrorMail(e);

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

    /**
     * 添加错误日志
     *
     * @param context
     * @param errorMessage
     */
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

    /**
     * 发送错误 mail
     *
     *  dev 环境不发送错误邮件
     *
     *  邮件会根据 spring 具体的 spring.profiles.active 来区分不同环境下的邮件提醒
     *
     * @param throwable
     */
    private void sendErrorMail(Throwable throwable) {
        MailUtil.sendSystemError(SUBJECT, ExceptionUtils.getMessage(throwable), environment);
    }

    /**
     * 去执行任务
     *
     * @param context
     */
    protected abstract void doProcess(JobExecutionContext context);
}
