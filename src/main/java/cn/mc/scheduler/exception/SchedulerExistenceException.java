package cn.mc.scheduler.exception;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.constants.SchedulerCodeConstants;

/**
 * @auther sin
 * @time 2018/2/5 15:32
 */
public class SchedulerExistenceException extends SchedulerNewException  {

    /**
     * 构造器
     * <p>
     * 需要传入一个 message，错误异常信息
     * 提示给开发者。
     *
     * @param message 错误异常信息
     */
    public SchedulerExistenceException(String message) {
        super(message);
    }

    @Override
    public int getCode() {
        return SchedulerCodeConstants.SCHEDULER_JOB_EXISTENCE;
    }
}
