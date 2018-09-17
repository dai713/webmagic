package cn.mc.scheduler.base;

import cn.mc.core.client.FileClient;
import cn.mc.core.client.FileClientProperties;
import cn.mc.core.dataObject.work.FilterWordDO;
import cn.mc.core.mail.MailUtil;
import cn.mc.core.mybatis.Field;
import cn.mc.core.utils.*;
import cn.mc.scheduler.SchedulerProperties;
import cn.mc.scheduler.mapper.FilterWordMapper;
import cn.mc.scheduler.review.ArticleReviewSubscribe;
import cn.mc.scheduler.service.SystemKeywordsService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 定时任务 init
 *
 * @auther sin
 * @time 2018/2/23 11:20
 */
@Component
public class SchedulerInit implements ApplicationRunner {

    private static final String REVIEW_SERVICE_HEARTBEAT_SUBJECT = "审核服务挂了！！！";
    private static final String REVIEW_SERVICE_HEARTBEAT_ERROR_MESSAGE = "心跳检测服务挂了！！！";
    private static final Integer REVIEW_SERVICE_HEARTBEAT_SLEEP_TIME = 1000 * 60;

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerInit.class);

    @Autowired
    private Environment environment;

    @Autowired
    private SystemKeywordsService systemKeywordsService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        // 初始化 FilterWord
        initFilterWord();

        // 初始化 FileCenter
        FileClient.init(BeanManager.getBean(FileClientProperties.class));

        // 审核服务 测试连接
//        reviewServiceTestConnection();

        // 新闻审核 - 文章审核订阅
        BeanManager.getBean(ArticleReviewSubscribe.class).subscribe();

        // SnowflakeIDGenerate init test
        Long nextId = new SnowflakeIDGenerate().nextId();
        LOGGER.info("SnowflakeIDGenerate nextId test. nextId {} ", nextId);

        LOGGER.info("初始化 keywords 缓存 begin");
        systemKeywordsService.refreshCache();
        LOGGER.info("初始化 keywords 缓存 end");
    }

    private void initFilterWord() {

        long now = System.currentTimeMillis();
        LOGGER.info("filter word 初始化开始！");
        FilterWordMapper filterWordMapper = BeanManager.getBean(FilterWordMapper.class);
        List<FilterWordDO> filterWordDOList = filterWordMapper.listAll(
                FilterWordDO.STATUS_RELEASE, new Field("keywords"));

        if (CollectionUtils.isEmpty(filterWordDOList)) {
            LOGGER.info("filter word 初始化结束！keywords {} 条！耗时 {} 毫秒！",
                    filterWordDOList.size(), System.currentTimeMillis() - now);
            return;
        }
        Set<String> keywordsSets = CollectionUtil.buildSet(
                filterWordDOList, String.class, "keywords");

        SensitiveWordUtil.init(keywordsSets);
        LOGGER.info("filter word 初始化结束！keywords {} 条！耗时 {} 毫秒！",
                filterWordDOList.size(), System.currentTimeMillis() - now);
    }

    private void reviewServiceTestConnection() {
        LOGGER.info("测试连接，新闻审核服务！开始！");

        boolean heartbeat = reviewServiceHeartbeat();

        LOGGER.info("测试连接，新闻审核服务！结束！心跳 {} !", heartbeat);
        Assert.isTrue(heartbeat, "启动失败！ 连接测试不成功！");

        // 如果启动成功，开启心跳检测
        reviewServiceHeartbeatStart();
    }


    private void reviewServiceHeartbeatStart() {

        new Thread(() -> {
            long lastTime = System.currentTimeMillis();
            while (true) {
                long now = System.currentTimeMillis();
                if (!(now - lastTime > REVIEW_SERVICE_HEARTBEAT_SLEEP_TIME)) {
                    continue;
                }
                lastTime = now;
                boolean heartbeat = reviewServiceHeartbeat();
                if (!heartbeat) {
                    LOGGER.error("审核服务挂了!");
                    MailUtil.sendSystemError(REVIEW_SERVICE_HEARTBEAT_SUBJECT,
                            REVIEW_SERVICE_HEARTBEAT_ERROR_MESSAGE,
                            BeanManager.getBean(Environment.class));
                }
            }
        }).start();
    }

    private boolean reviewServiceHeartbeat() {
        SchedulerProperties schedulerProperties
                = BeanManager.getBean(SchedulerProperties.class);

        String url = schedulerProperties.videoImageReviewServer;
        CloseableHttpResponse httpResponse
                = Http2Util.httpGetWithHttpResponse(url, Collections.EMPTY_MAP);

        HttpUtil.closeQuietly(httpResponse);
        return httpResponse != null;
    }
}
