package cn.mc.scheduler.exception;

import cn.mc.core.exception.SchedulerNewException;

/**
 * 定时任务 终止
 *
 * @author Sin
 * @time 2018/8/7 下午3:24
 */
public class SchedulerTerminationTaskException extends SchedulerNewException {

    /**
     * 构造器
     * <p>
     * 需要传入一个 message，错误异常信息
     * 提示给开发者。
     *
     * @param message 错误异常信息
     */
    public SchedulerTerminationTaskException(String message) {
        super(message);
    }
}
