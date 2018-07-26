package cn.mc.scheduler.dataObject;

import cn.mc.core.entity.BaseEntity;

import java.util.Map;

/**
 * @auther sin
 * @time 2018/2/2 17:45
 */
public class QrtzJobDetailsDO extends BaseEntity {

    private String schedName;
    private String jobName;
    private String jobGroup;
    private String description;
    private String jobClassName;
    private String isDurable;
    private String isNonconcurrent;
    private String isUpdateData;
    private String requestsRecovery;
    private Object jobData;

    @Override
    public String toString() {
        return "QrtzJobDetailsDO{" +
                "schedName='" + schedName + '\'' +
                ", jobName='" + jobName + '\'' +
                ", jobGroup='" + jobGroup + '\'' +
                ", description='" + description + '\'' +
                ", jobClassName='" + jobClassName + '\'' +
                ", isDurable='" + isDurable + '\'' +
                ", isNonconcurrent='" + isNonconcurrent + '\'' +
                ", isUpdateData='" + isUpdateData + '\'' +
                ", requestsRecovery='" + requestsRecovery + '\'' +
                ", jobData=" + jobData +
                '}';
    }

    public String getSchedName() {
        return schedName;
    }

    public void setSchedName(String schedName) {
        this.schedName = schedName;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJobClassName() {
        return jobClassName;
    }

    public void setJobClassName(String jobClassName) {
        this.jobClassName = jobClassName;
    }

    public String getIsDurable() {
        return isDurable;
    }

    public void setIsDurable(String isDurable) {
        this.isDurable = isDurable;
    }

    public String getIsNonconcurrent() {
        return isNonconcurrent;
    }

    public void setIsNonconcurrent(String isNonconcurrent) {
        this.isNonconcurrent = isNonconcurrent;
    }

    public String getIsUpdateData() {
        return isUpdateData;
    }

    public void setIsUpdateData(String isUpdateData) {
        this.isUpdateData = isUpdateData;
    }

    public String getRequestsRecovery() {
        return requestsRecovery;
    }

    public void setRequestsRecovery(String requestsRecovery) {
        this.requestsRecovery = requestsRecovery;
    }

    public Object getJobData() {
        return jobData;
    }

    public void setJobData(Object jobData) {
        this.jobData = jobData;
    }
}
