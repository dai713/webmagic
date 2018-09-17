package cn.mc.scheduler.review.rest;

import cn.mc.core.constants.CodeConstants;
import cn.mc.core.dataObject.ReviewFailDO;
import cn.mc.core.mybatis.Update;
import cn.mc.core.result.Result;
import cn.mc.scheduler.SchedulerProperties;
import cn.mc.scheduler.mapper.SchedulerLogsMapper;
import cn.mc.scheduler.util.ImgUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * 图片校验
 *
 * @auther xl
 * @time 2018/6/29 20:59
 */
@Component
public class CheckImgClient {
    private static Logger logger = LoggerFactory.getLogger(CheckImgClient.class);

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private SchedulerProperties schedulerProperties;

    @Autowired
    private SchedulerLogsMapper schedulerLogsMapper;

    private static final String IMG_REVIEW_SERVICE_URL = "/grabber/imageReviewSimple";

    private boolean checkImgDown(String imgUrl){
        try {
            InputStream inputStream= ImgUtil.getInputStream(imgUrl);
            BufferedImage sourceImg = ImageIO.read(inputStream);
            if(!StringUtils.isEmpty(sourceImg)){
                //如果宽度和高度小于5则直接移除此图片
                if(sourceImg.getWidth()<=10 && sourceImg.getHeight()<=10){
                    return false;
                }
            }
        } catch (Exception e) {
             logger.error("图片流对象获取不到返回审核不通过");
             return  false;
        }
        return true;
    }

    public boolean checkImgUrl(String url,Long newsId) {
        logger.info("开始审核图片:" + url);
        Boolean checkResult=checkImgDown(url);
        if(checkResult){
            Result result = getCheckImgData(url);
            JSONObject jsonObject = JSON.parseObject(result.getResult().toString());
            Boolean reviewResult=jsonObject.getBoolean("reviewValue");
            Double reviewValue=jsonObject.getDoubleValue("reviewResult");
            //如果校验失败 记录失败的信息
            if(reviewResult==false){
                ReviewFailDO reviewFailDO=new ReviewFailDO();
                reviewFailDO.setNewsId(newsId);
                reviewFailDO.setImageUrl(url);
                reviewFailDO.setReviewValue(reviewValue);
                logger.info("审核图片reviewValue:" + reviewValue);
                schedulerLogsMapper.insertReviewFailLog(Update.copyWithoutNull(reviewFailDO));
            }
            checkResult=reviewResult;
        }
        logger.info("审核图片结果:" + checkResult);
        return checkResult;
    }

    private Result getCheckImgData(String url) {
        String authServerUrl = schedulerProperties.videoImageReviewServer;
        String requestUrl = String.format("%s%s", authServerUrl, IMG_REVIEW_SERVICE_URL);
        try {
            ResponseEntity<Result> responseEntity = restTemplate.postForEntity(
                    URI.create(requestUrl), url, Result.class);

            return responseEntity.getBody();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
