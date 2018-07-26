package cn.mc.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 定时任务配置文件
 *
 * @auther sin
 * @time 2018/4/21 15:21
 */
@Configuration
public class SchedulerProperties {

    /**
     * 阿里云配置 endPoint
     */
    @Value("${scheduler.oss.end-point}")
    public String endPoint;
    /**
     * 阿里云 accessKeyId
     */
    @Value("${scheduler.oss.access-key-id}")
    public String accessKeyId;
    /**
     * 阿里云 accessKeySecret
     */
    @Value("${scheduler.oss.access-key-secret}")
    public String accessKeySecret;
    /**
     * 阿里云 bucket
     */
    @Value("${scheduler.oss.bucket-name}")
    public String bucketName;
    /**
     * 构建 url 地址
     */
    @Value("${scheduler.oss.build-url}")
    public String buildUrl;

    ///
    /// 安全过滤

    /**
     * 阿里过滤文本安全key
     */
    @Value("${text-filter.security.access-key-id}")
    public String securityAccessKeyId;
    /**
     * 阿里过滤文本安全accessKeySecret
     */
    @Value("${text-filter.security.access-key-secret}")
    public String securityAccessKeySecret;
    /**
     * 阿里过滤文本安全站点
     */
    @Value("${text-filter.security.region-id}")
    public String securityRegionId;

    ///
    /// 内容审核

    /**
     * 图片审核服务
     */
    @Value("${sys.video-image-review-server}")
    public String videoImageReviewServer;
}