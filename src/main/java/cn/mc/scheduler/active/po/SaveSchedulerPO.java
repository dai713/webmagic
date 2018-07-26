package cn.mc.scheduler.active.po;

import javax.validation.constraints.NotNull;

/**
 * 保存一个定时任务
 *
 * @auther sin
 * @time 2018/2/2 14:48
 */
public class SaveSchedulerPO {

    /**
     * 任务名称
     */
    @NotNull
    private String jobName;
    /**
     * 任务分组
     */
    @NotNull
    private String jobGroup;
    /**
     * 触发器名称
     */
    @NotNull
    private String triggerName;
    /**
     * 触发器类型
     */
    @NotNull
    private String triggerType;
    /**
     * 触发器分组
     */
    @NotNull
    private String triggerGroup;
    /**
     * 触发器表达式
     */
    @NotNull
    private String triggerExpression;
    /**
     * 描述
     */
    @NotNull
    private String description;

    @Override
    public String toString() {
        return "AddSchedulerPO{" +
                "triggerType='" + triggerType + '\'' +
                ", triggerName='" + triggerName + '\'' +
                ", triggerGroup='" + triggerGroup + '\'' +
                ", jobName='" + jobName + '\'' +
                ", jobGroup='" + jobGroup + '\'' +
                ", triggerExpression='" + triggerExpression + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
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

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getTriggerExpression() {
        return triggerExpression;
    }

    public void setTriggerExpression(String triggerExpression) {
        this.triggerExpression = triggerExpression;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
