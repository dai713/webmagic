package cn.mc.scheduler;

import com.aliyun.openservices.ons.api.Consumer;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * MQ 配置
 *
 * @author sin
 * @time 2018/7/13 15:30
 */
@Configuration
public class MQConfig {

    @Bean
    @ConfigurationProperties("sys.mq.news-review")
    public MQProperties newsReviewMQProperties() {
        return new MQProperties();
    }


    ///
    /// news-review

    @Bean
    public Producer newsReviewProducer(@Qualifier("newsReviewMQProperties") MQProperties mqProperties) {
        Properties properties = getProducerProperties(mqProperties);
        Producer producer = ONSFactory.createProducer(properties);
        producer.start();
        return producer;
    }

    @Bean
    public Consumer newsReviewConsumer(@Qualifier("newsReviewMQProperties") MQProperties mqProperties) {
        Properties properties = getCustomerProperties(mqProperties);
        Consumer consumer = ONSFactory.createConsumer(properties);
        return consumer;
    }


    ///
    /// private

    private Properties getProducerProperties(MQProperties mqProperties) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.AccessKey, mqProperties.getAccessKeyId());
        properties.put(PropertyKeyConst.SecretKey, mqProperties.getAccessKeySecret());
        properties.put(PropertyKeyConst.ProducerId, mqProperties.getProducerId());
        properties.setProperty(PropertyKeyConst.SendMsgTimeoutMillis, "3000");
        properties.put(PropertyKeyConst.ONSAddr, mqProperties.getOnsAddr());
        return properties;
    }

    private Properties getCustomerProperties(MQProperties mqProperties) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.AccessKey, mqProperties.getAccessKeyId());
        properties.put(PropertyKeyConst.SecretKey, mqProperties.getAccessKeySecret());
        properties.put(PropertyKeyConst.ConsumerId, mqProperties.getConsumerId());
        properties.put(PropertyKeyConst.ONSAddr, mqProperties.getOnsAddr());
        return properties;
    }
}
