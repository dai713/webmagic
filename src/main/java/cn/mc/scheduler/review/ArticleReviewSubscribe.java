package cn.mc.scheduler.review;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.mybatis.Field;
import cn.mc.scheduler.mapper.NewsMapper;
import cn.mc.scheduler.mq.MQTemplate;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

/**
 * 文章审核 订阅
 *
 * @author sin
 * @time 2018/7/13 17:15
 */
@Component
public class ArticleReviewSubscribe {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArticleReviewSubscribe.class);

    @Autowired
    private MQTemplate mqTemplate;
    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private AutomaticReviewService automaticReviewService;
    @Autowired
    private AutomaticReviewVideoService automaticReviewVideoService;

    public void subscribe() {
        mqTemplate.subArticleMessage(this::consume);
    }

    private Action consume(Message message, ConsumeContext context) {
        JSONObject jsonObject = JSON.parseObject(
                new String(message.getBody(), Charset.forName("UTF-8")));

        if (!jsonObject.containsKey("newsId")) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("新闻审核订阅, 发送的数据不正确！body {}", jsonObject);
            }
            return Action.CommitMessage;
        }

        Long newsId = Long.valueOf(String.valueOf(jsonObject.get("newsId")));
        NewsDO newsDO = newsMapper.selectById(newsId, new Field());

        // 不处理空数据
        if (newsDO == null) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("新闻审核订阅, 新闻不存在！newsId {} ", newsId);
            }
            return Action.CommitMessage;
        }

        if (MQTemplate.ARTICLE_TAG.equals(message.getTag())
                || MQTemplate.PICTURES_TAG.equals(message.getTag())) {
            // 文章审核、图集
            automaticReviewService.reviewArticle(ImmutableList.of(newsDO));
        } else if (MQTemplate.VIDEO_TAG.equals(message.getTag())) {
            // 视频审核
            automaticReviewVideoService.reviewVideo(ImmutableList.of(newsDO));
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("审核完成. msgId {} tag {} newsId {} ",
                    message.getMsgID(), message.getTag(), newsId);
        }
        return Action.CommitMessage;
    }
}
