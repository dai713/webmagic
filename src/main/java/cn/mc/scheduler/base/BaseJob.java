package cn.mc.scheduler.base;

import cn.mc.core.exception.SchedulerNewException;

/**
 * @auther sin
 * @time 2018/2/2 10:15
 */
public abstract class BaseJob {

    public abstract void execute() throws SchedulerNewException;
}
