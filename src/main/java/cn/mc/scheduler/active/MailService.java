package cn.mc.scheduler.active;

import org.springframework.stereotype.Service;

/**
 * @auther sin
 * @time 2018/2/3 09:56
 */
@Service
public class MailService {

//    @Autowired
//    private MailSender mailSender;
//
//    @Value("${spring.mail.username}")
//    private String sender; //读取配置文件中的参数
//
    public void sendSystemFailMessage(String text) {
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setFrom(sender);
//        message.setTo("cherishsince@aliyun.com");
//        message.setSubject("[Browser系统错误] - 异常提醒");
//        message.setText(text);
//        mailSender.send(message);
    }

    public void sendBusinessFailMessage(String text) {
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setFrom(sender);
//        message.setTo("cherishsince@aliyun.com");
//        message.setSubject("[Browser业务错误] - 异常提醒");
//        message.setText(text);
//        mailSender.send(message);
    }
}
