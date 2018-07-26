package cn.mc.scheduler.active.po;

import javax.validation.constraints.NotNull;

/**
 * 更新一个定时任务
 *
 * @auther sin
 * @time 2018/2/2 15:33
 */
public class UpdateSchedulerPO {

    /**
     * 定时器 编号
     */
    @NotNull
    private Long schedulerId;
    /**
     * 触发器 名称
     */
    @NotNull
    private String triggerName;
    /**
     * 触发器 类型
     */
    @NotNull
    private String triggerType;
    /**
     * 触发器 分组
     */
    @NotNull
    private String triggerGroup;
    /**
     * 触发器 表达式
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
        return "UpdateSchedulerPO{" +
                "schedulerId='" + schedulerId + '\'' +
                ", triggerType='" + triggerType + '\'' +
                ", triggerName='" + triggerName + '\'' +
                ", triggerGroup='" + triggerGroup + '\'' +
                ", triggerExpression='" + triggerExpression + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public Long getSchedulerId() {
        return schedulerId;
    }

    public void setSchedulerId(Long schedulerId) {
        this.schedulerId = schedulerId;
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
