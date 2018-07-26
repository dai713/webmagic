package cn.mc.scheduler;

/**
 * MQ 属性配置
 *
 * @author sin
 * @time 2018/7/13 16:02
 */
public class MQProperties {

    private String accessKeyId;
    private String accessKeySecret;
    private String onsAddr;
    private String topic;
    private String producerId;
    private String consumerId;

    @Override
    public String toString() {
        return "MQProperties{" +
                "accessKeyId='" + accessKeyId + '\'' +
                ", accessKeySecret='" + accessKeySecret + '\'' +
                ", onsAddr='" + onsAddr + '\'' +
                ", topic='" + topic + '\'' +
                ", producerId='" + producerId + '\'' +
                ", consumerId='" + consumerId + '\'' +
                '}';
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getOnsAddr() {
        return onsAddr;
    }

    public void setOnsAddr(String onsAddr) {
        this.onsAddr = onsAddr;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getProducerId() {
        return producerId;
    }

    public void setProducerId(String producerId) {
        this.producerId = producerId;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }
}
