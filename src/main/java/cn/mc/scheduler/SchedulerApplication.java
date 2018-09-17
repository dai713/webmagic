package cn.mc.scheduler;

import cn.mc.core.client.FileClientProperties;
import cn.mc.core.converter.AppHttpMessageConverter;
import cn.mc.core.mysql.DataSourceContextHolder;
import cn.mc.core.mysql.DataSourceType;
import cn.mc.core.utils.BeanManager;
import cn.mc.core.utils.HttpUtil;
import cn.mc.scheduler.base.BrowserSchedulerFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * 定时任务
 *
 * @auther sin
 * @time 2018/1/4 11:22
 */
@Configuration
@EnableScheduling
@SpringBootApplication(
        exclude = {
                DataSourceAutoConfiguration.class,
        },
        scanBasePackages = "cn.mc"
)
@ComponentScan(basePackages = "cn.mc")
public class SchedulerApplication {

    public static void main(String[] args) {
        System.setProperty("user.timezone","Asia/Shanghai");
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        SpringApplication.run(SchedulerApplication.class);
    }

    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(10000);
        httpRequestFactory.setConnectTimeout(10000);
        httpRequestFactory.setReadTimeout(10000);
        httpRequestFactory.setHttpClient(HttpUtil.getHttpClient());
        return new RestTemplate(httpRequestFactory);
    }

    @Bean
    public BeanManager beanManager(ApplicationContext applicationContext) {
        BeanManager beanManager = new BeanManager();
        beanManager.setApplicationContext(applicationContext);
        return beanManager;
    }

    @Bean
    @ConfigurationProperties("scheduler.oss")
    public FileClientProperties fileClientProperties() {
        return new FileClientProperties();
    }

    @Bean
    public AppHttpMessageConverter appHttpMessageConverter() {
        return AppHttpMessageConverter.create();
    }

    @Bean
    BrowserSchedulerFactoryBean browserSchedulerFactoryBean(
            DataSourceContextHolder dataSourceContextHolder) {

        // schedulerFactoryBean
        BrowserSchedulerFactoryBean factoryBean = new BrowserSchedulerFactoryBean();

        // configLocation
        factoryBean.setConfigLocation(new ClassPathResource("schedule/quartz-cluster.properties"));

        // 手动启动
        factoryBean.setStartupDelay(-1);

        // 设置 data source
        factoryBean.setDataSource(DataSourceContextHolder.getDataSource(DataSourceType.DB_SCHEDULER));

        // 延迟 2 秒
        factoryBean.setStartupDelay(2);

        //applicationContextSchedulerContextKey
        factoryBean.setApplicationContextSchedulerContextKey("applicationContext");
        return factoryBean;
    }
}
