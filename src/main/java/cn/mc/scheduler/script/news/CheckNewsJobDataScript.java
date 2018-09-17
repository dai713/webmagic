package cn.mc.scheduler.script.news;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.core.mail.MailUtil;
import cn.mc.core.utils.BeanManager;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 检查抓取数据任务
 *
 * @author xl
 * @date 2018-8-22 下午18:17
 */
@Component
public class CheckNewsJobDataScript extends BaseJob {

    @Autowired
    private RedisUtil redisUtil;

    public static  String redisKey="cacheDataTime";

    @Override
    public void execute() throws SchedulerNewException {

        Map<String, String>  map1=redisUtil.hmget(redisKey);
        for (String key : map1.keySet()) {
            String time1=map1.get(key);
            if(StringUtils.isEmpty(time1)){
                continue;
            }
            Long s = (System.currentTimeMillis() - new Long(time1)) / (1000 * 60);
            //时间大于4小时 则发送邮件告警
            if(s>=240){
                Environment environment = BeanManager.getBean(Environment.class);
                String subject="有爬虫抓取新闻最近:"+s+"分钟没有更新数据了";
                String comment=subject+"抓取的类为:"+key;
                MailUtil.sendSystemError(subject,comment,environment);
            }
        }

    }
}
