package cn.mc.scheduler;

import cn.mc.core.converter.AppHttpMessageConverter;
import cn.mc.core.mysql.DataSourceContextHolder;
import cn.mc.core.mysql.DataSourceType;
import cn.mc.core.utils.BeanManager;
import cn.mc.scheduler.base.BrowserSchedulerFactoryBean;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

/**
 * 定时任务
 *
 * @auther sin
 * @time 2018/1/4 11:22
 */
@Configurable
@SpringBootApplication(
        exclude = {
                DataSourceAutoConfiguration.class,
        },
        scanBasePackages = "cn.mc"
)
public class SchedulerApplicationTest {

    public static void main(String[] args) {
        System.setProperty("user.timezone","Asia/Shanghai");
        SpringApplication.run(SchedulerApplicationTest.class);
    }

    @Bean
    public BeanManager beanManager(ApplicationContext applicationContext) {
        BeanManager beanManager = new BeanManager();
        beanManager.setApplicationContext(applicationContext);
        return beanManager;
    }

    @Bean
    public AppHttpMessageConverter appHttpMessageConverter() {
        return AppHttpMessageConverter.create();
    }

    @Bean
    BrowserSchedulerFactoryBean browserSchedulerFactoryBean(
            DataSourceContextHolder dataSourceContextHolder) {
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
