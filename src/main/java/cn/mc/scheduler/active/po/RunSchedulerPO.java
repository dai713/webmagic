package cn.mc.scheduler.active.po;

import javax.validation.constraints.NotNull;

/**
 * 运行一个
 *
 * @auther sin
 * @time 2018/2/2 15:43
 */
public class RunSchedulerPO {

    public static final Integer RUN_TYPE_JOB_DETAIL = 0;
    public static final Integer RUN_TYPE_BEAN_MANAGER = 1;

    /**
     * 运行类型： 0、jobDetail 1、BeanManager 运行
     */
    @NotNull
    private Integer runType;
    @NotNull
    private String jobGroup;

    @Override
    public String toString() {
        return "RunSchedulerPO{" +
                "runType=" + runType +
                ", jobGroup='" + jobGroup + '\'' +
                '}';
    }

    public Integer getRunType() {
        return runType;
    }

    public void setRunType(Integer runType) {
        this.runType = runType;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }
}
