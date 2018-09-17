package cn.mc.scheduler.base;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

public class BrowserSchedulerFactoryBean extends SchedulerFactoryBean {

    @Override
    protected void startScheduler(final Scheduler scheduler, final int startupDelay) throws SchedulerException {

        // 当时间小于0时，手动进行启动。
        if (startupDelay < 0) {
            logger.info("Does not automatically start!");
        } else {
            super.startScheduler(scheduler, startupDelay);
        }
    }
}
