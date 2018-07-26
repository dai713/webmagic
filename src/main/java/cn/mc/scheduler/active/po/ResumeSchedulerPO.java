package cn.mc.scheduler.active.po;

import javax.validation.constraints.NotNull;

/**
 * 重启
 *
 * @auther sin
 * @time 2018/2/2 16:28
 */
public class ResumeSchedulerPO {

    /**
     * 触发器 名称
     */
    @NotNull
    private String triggerName;
    /**
     * 触发器 分组
     */
    @NotNull
    private String triggerGroup;

    @Override
    public String toString() {
        return "ResumeSchedulerPO{" +
                "triggerName='" + triggerName + '\'' +
                ", triggerGroup='" + triggerGroup + '\'' +
                '}';
    }

    public String getTriggerName() {
        return triggerName;
    }

    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }

    public String getTriggerGroup() {
        return triggerGroup;
    }

    public void setTriggerGroup(String triggerGroup) {
        this.triggerGroup = triggerGroup;
    }
}
