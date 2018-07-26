package cn.mc.scheduler.mq;

import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.MQProperties;
import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.ons.api.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * mq 模板
 *
 * @author sin
 * @time 2018/7/13 16:42
 */
@Component
public class MQTemplate {

    private static final Logger LOGGER = LoggerFactory.getLogger(MQTemplate.class);

    @Autowired
    @Qualifier("newsReviewProducer")
    private Producer newsReviewProducer;
    @Autowired
    @Qualifier("newsReviewMQProperties")
    private MQProperties newsReviewMQProperties;
    @Autowired
    @Qualifier("newsReviewConsumer")
    private Consumer consumer;
    @Autowired
    @Qualifier("newsReviewMQProperties")
    private MQProperties mqProperties;


    public static final String ARTICLE_TAG = "ArticleTag";
    public static final String VIDEO_TAG = "VideoTag";
    public static final String PICTURES_TAG = "PicturesTag";

    public void sendNewsReviewMessage(@NotNull String messageTag,
                                      @NotNull Map<String, Object> params) {

        Message msg = new Message(
                newsReviewMQProperties.getTopic(),
                messageTag,
                JSON.toJSONString(params).getBytes());

        String key = "ARTICLE_REVIEW" + IDUtil.getNewID();
        msg.setKey(key);

        SendResult sendResult = newsReviewProducer.send(msg);

        System.out.println(new Date()
                + " Send mq message success. Topic is:" + msg.getTopic()
                + " msgId is: " + sendResult.getMessageId());
    }

    public void subArticleMessage(MessageListener messageListener) {
        String subExpression = "*";

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("订阅成功！ expression: {} ", subExpression);
        }

        consumer.subscribe(
                mqProperties.getTopic(),
                subExpression,
                messageListener);
        consumer.start();
    }
}
