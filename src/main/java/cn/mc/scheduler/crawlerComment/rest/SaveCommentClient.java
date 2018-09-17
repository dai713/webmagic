package cn.mc.scheduler.crawlerComment.rest;

import cn.mc.core.result.Result;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.scheduler.SchedulerProperties;
import cn.mc.scheduler.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class SaveCommentClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveCommentClient.class);
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private SchedulerProperties schedulerProperties;
    @Autowired
    private RedisUtil redisUtil;

    private static final String regEx_html = "<[^>]+>"; // 定义HTML标签的正则表达式

    private static final String SAVE_GRAB_COMMENT = "/comment/saveGrabComment";

    public boolean saveGrabComment(CommentSavePO commentSavePO) {
        //去掉所有html标签
        Pattern p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
        Matcher m_html = p_html.matcher(commentSavePO.getComments());
        String commentStr = m_html.replaceAll(""); // 过滤html标签
        commentSavePO.setComments(commentStr);
        //校验缓存信息
        boolean isComment=isComment(commentSavePO.getNewsId(),commentSavePO.getComments());
        //如果不存在则保存
        if(isComment==false){
            LOGGER.error("开始保存评论信息:"+commentSavePO.getComments()+":newsId="+commentSavePO.getNewsId());
            try{
                String authServerUrl = schedulerProperties.clearCacheAppServer;
                String requestUrl = String.format("%s%s", authServerUrl, SAVE_GRAB_COMMENT);
                ResponseEntity<Result> responseEntity = restTemplate.postForEntity(
                        URI.create(requestUrl), commentSavePO, Result.class);
                Result result=responseEntity.getBody();
                Integer code=result.getCode();
                //成功
                if(code==0){
                    System.out.println("成功");
                }else{
                    System.out.println("失败");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return isComment;
    }
    private boolean isComment(Long newsId,String comment){
        String commentDataKey = EncryptUtil.encrypt(comment, "md5");
        String exitsCommentKey=newsId+"_commentexits";
        //首先获取缓存中的信息
        Map<String,String> map=redisUtil.hmget(exitsCommentKey);
        String key=map.get(commentDataKey);
        if(StringUtils.isEmpty(key)){
            map.put(commentDataKey,"true");
            redisUtil.hmset(exitsCommentKey,map,60 * 60 * 24);
            return  false;
        }
        return  true;
    }
}
