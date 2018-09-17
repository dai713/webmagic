package cn.mc.scheduler.script.wangYi;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.entity.Page;
import cn.mc.core.exception.SchedulerNewException;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.CommonUtil;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.mapper.NewsMapper;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 网易视频 data-key 数据修复
 *
 * @author sin
 * @time 2018/7/31 08:55
 */
@Component
public class WangYiVideoDataKeyScript extends BaseJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(WangYiVideoDataKeyScript.class);

    @Autowired
    private NewsMapper newsMapper;

    private int pageSize = 100;

    @Override
    public void execute() throws SchedulerNewException {
        try {
            int newsType = NewsDO.NEWS_TYPE_VIDEO;
            String newsSourceUrl = "https://c.m.163.com/";

            Page page = new Page();
            page.setSize(pageSize);
            LOGGER.info("视频修复脚本 start index {}", page.getIndex());

            for (int i = 0; i < 1000000; i++) {
                page.setIndex(i + 1);

                List<NewsDO> newsDOList = newsMapper.selectByNewsTypeLikeResourceUrl(
                        newsType, newsSourceUrl, page, new Field("newsId",
                                "title", "newsSourceUrl", "dataKey"));

                if (CollectionUtils.isEmpty(newsDOList)) {
                    break;
                }

                for (NewsDO newsDO : newsDOList) {
                    String title = newsDO.getTitle();
                    if (StringUtils.isEmpty(title)) {
                        continue;
                    }

                    String newDataKey = EncryptUtil.encrypt(title, "MD5");
                    newsMapper.updateById(newsDO.getNewsId(), Update.update("dataKey", newDataKey));
                }

                LOGGER.info("视频修复脚本 \r\n {}", JSON.toJSONString(page));
            }

            LOGGER.info("视频修复脚本 修复完成! \r\n {}", JSON.toJSONString(page));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
