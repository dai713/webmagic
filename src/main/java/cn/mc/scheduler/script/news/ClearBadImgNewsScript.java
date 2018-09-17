package cn.mc.scheduler.script.news;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.jobs.rest.CacheClearClient;
import cn.mc.scheduler.jobs.rest.CacheClearNewsDO;
import cn.mc.scheduler.jobs.rest.CacheClearNewsPO;
import cn.mc.scheduler.mapper.NewsMapper;
import cn.mc.scheduler.util.ImgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * 清除新闻图片有问题的数据任务
 *
 * @author xl
 * @date 2018-8-22 下午18:17
 */
@Component
public class ClearBadImgNewsScript extends BaseJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClearBadImgNewsScript.class);
    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private CacheClearClient cacheClearClient;
    @Override
    public void execute() throws SchedulerNewException {

        //有问题的img news集合
        List<CacheClearNewsPO> clearList=new ArrayList<>();
        //查询最近一周的新闻
        List<CacheClearNewsDO> news=newsMapper.selectByDataThisWeek();

        for(CacheClearNewsDO cacheClearNewsDO:news){
            LOGGER.info("开始执行图片:"+cacheClearNewsDO.getImageUrl());
            if(!StringUtils.isEmpty(cacheClearNewsDO.getImageUrl())){
               boolean checkImg= checkImgDown(cacheClearNewsDO.getImageUrl());
               //删除新闻以及相关内容
               if(checkImg==false){
                   CacheClearNewsPO cacheClearNewsPO=new CacheClearNewsPO();
                   LOGGER.info("删除有问题图片的新闻:"+cacheClearNewsDO.getNewsId());
                   newsMapper.deleteNewsById(cacheClearNewsDO.getNewsId());
                   cacheClearNewsPO.setNewsId(cacheClearNewsDO.getNewsId());
                   cacheClearNewsPO.setNewsType(cacheClearNewsDO.getNewsType());
                   clearList.add(cacheClearNewsPO);
               }
            }
        }
        if(clearList.size()>0){
            cacheClearClient.sendClearCacheNews(clearList);
        }
        LOGGER.info("总共删除图片有问题的新闻:"+clearList.size()+"条");

    }
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
            return  false;
        }
        return true;
    }
}
