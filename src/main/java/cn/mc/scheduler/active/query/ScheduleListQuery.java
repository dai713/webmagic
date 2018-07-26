package cn.mc.scheduler.active.query;

/**
 * @auther sin
 * @time 2018/2/7 16:52
 */
public class ScheduleListQuery {

    /**
     * 任务名称
     */
    private String jobName;
    /**
     * 分类
     */
    private String status;

    @Override
    public String toString() {
        return "ScheduleListQuery{" +
                "jobName='" + jobName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
