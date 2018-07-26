package cn.mc.scheduler.active;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.core.result.Result;
import cn.mc.core.result.ResultBuilder;
import cn.mc.scheduler.active.po.ResumeSchedulerPO;
import cn.mc.scheduler.active.po.RunSchedulerPO;
import cn.mc.scheduler.active.po.SaveSchedulerPO;
import cn.mc.scheduler.active.po.UpdateSchedulerPO;
import cn.mc.scheduler.active.query.ScheduleListQuery;
import cn.mc.scheduler.constants.SchedulerCodeConstants;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @auther sin
 * @time 2018/2/1 08:59
 */
@RestController
public class SchedulerController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SchedulerService schedulerService;

    /**
     * 定时器 - 列表
     *
     * @param index 分页 index
     */
    @PatchMapping("scheduler/{index}")
    public Result schedulerList(@PathVariable Integer index,
                                @RequestBody ScheduleListQuery scheduleListQuery) {
        return ResultBuilder.Build()
                .setResult(schedulerService.listScheduler(index, scheduleListQuery))
                .build();
    }

    /**
     * 定时器 - 保存
     *
     * @param saveSchedulerPO 需要保存的信息
     */
    @PostMapping(value = "scheduler")
    public Result saveScheduler(@RequestBody @Validated SaveSchedulerPO saveSchedulerPO) {
        try {
            schedulerService.saveScheduler(saveSchedulerPO);
        } catch (Exception e) {
            logger.error("scheduler ==> script job exception [system]. {}",
                    ExceptionUtils.getStackTrace(e));
            return ResultBuilder.Build()
                    .setCode(SchedulerCodeConstants.SCHEDULER_ERROR)
                    .setMessage(e.getMessage())
                    .build();
        }
        return ResultBuilder.Build().build();
    }


    /**
     * 定时器 - 更新
     *
     * @param updateSchedulerPO 需要更新的信息
     */
    @PutMapping("scheduler")
    public Result updateScheduler(@RequestBody @Validated UpdateSchedulerPO updateSchedulerPO) {
        try {
            schedulerService.updateScheduler(updateSchedulerPO);
        } catch (Exception e) {
            logger.error("scheduler ==> script job exception [system]. {}",
                    ExceptionUtils.getStackTrace(e));
            return ResultBuilder.Build()
                    .setCode(SchedulerCodeConstants.SCHEDULER_ERROR)
                    .setMessage(e.getMessage())
                    .build();
        }
        return ResultBuilder.Build().build();
    }

    /**
     * 定时器 - 删除
     *
     * @param jobName 任务名
     * @param jobGroup 任务 group
     */
    @DeleteMapping("scheduler/{jobName}/{jobGroup}")
    public Result removeScheduler(@PathVariable String jobName, @PathVariable String jobGroup) {
        try {
            schedulerService.removeScheduler(jobName, jobGroup);
        } catch (Exception e) {
            logger.error("scheduler ==> script job exception [system]. {}",
                    ExceptionUtils.getStackTrace(e));
            return ResultBuilder.Build()
                    .setCode(SchedulerCodeConstants.SCHEDULER_ERROR)
                    .setMessage(e.getMessage())
                    .build();
        }
        return ResultBuilder.Build().build();
    }

    /**
     * 定时器 - 暂停
     *
     * @param jobName 定时器名
     * @param jobGroup 定时器 group
     */
    @PatchMapping("scheduler/{jobName}/pause/{jobGroup}")
    public Result pauseScheduler(@PathVariable String jobName, @PathVariable String jobGroup) {
        try {
            schedulerService.pauseScheduler(jobName, jobGroup);
        } catch (Exception e) {
            logger.error("scheduler ==> script job exception [system]. {}",
                    ExceptionUtils.getStackTrace(e));
            return ResultBuilder.Build()
                    .setCode(SchedulerCodeConstants.SCHEDULER_ERROR)
                    .setMessage(e.getMessage())
                    .build();
        }
        return ResultBuilder.Build().build();
    }

    /**
     * 定时器 - 重启
     *
     * @param jobName 任务名
     * @param jobGroup 任务 group
     * @param resumeSchedulerPO 重启需要的信息
     */
    @PatchMapping("scheduler/{jobName}/resume/{jobGroup}")
    public Result schedulerResume(@PathVariable String jobName,
                                  @PathVariable String jobGroup,
                                  @RequestBody @Validated ResumeSchedulerPO resumeSchedulerPO) {
        try {
            schedulerService.resumeScheduler(jobName, jobGroup, resumeSchedulerPO);
        } catch (Exception e) {
            logger.error("scheduler ==> script job exception [system]. {}",
                    ExceptionUtils.getStackTrace(e));
            return ResultBuilder.Build()
                    .setCode(SchedulerCodeConstants.SCHEDULER_ERROR)
                    .setMessage(e.getMessage())
                    .build();
        }
        return ResultBuilder.Build().build();
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
     * @param runSchedulerPO
     * @return
     */
    @PatchMapping("scheduler/{jobName}/run")
    public Result schedulerRun(@PathVariable String jobName,
                               @RequestBody @Validated RunSchedulerPO runSchedulerPO) {
        try {
            schedulerService.runScheduler(jobName, runSchedulerPO);
        } catch (SchedulerNewException e) {
            logger.error("scheduler ==> script job exception [business]. {}",
                    ExceptionUtils.getStackTrace(e));
            return ResultBuilder.Build()
                    .setCode(SchedulerCodeConstants.SCHEDULER_BUSINESS)
                    .setMessage(e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("scheduler ==> script job exception [system]. {}",
                    ExceptionUtils.getStackTrace(e));
            return ResultBuilder.Build()
                    .setCode(SchedulerCodeConstants.SCHEDULER_ERROR)
                    .setMessage(e.getMessage())
                    .build();
        }
        return ResultBuilder.Build().build();
    }
}
