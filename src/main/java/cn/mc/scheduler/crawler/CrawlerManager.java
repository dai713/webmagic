package cn.mc.scheduler.crawler;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.utils.DateUtil;
import cn.mc.scheduler.mapper.NewsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author sin
 * @time 2018/7/19 14:47
 */
@Component
public class CrawlerManager {

    @Autowired
    private NewsMapper newsMapper;

    private static final int TOP_DAT = -30;

    /**
     * 根据单个： dataKeys 查询新闻 并且 > addTime
     *
     * @param dataKey
     * @param field
     * @return
     */
    public NewsDO listNewsDOByDataKey(String dataKey, Field field) {
        Date date = DateUtil.addDate(Calendar.DAY_OF_MONTH, TOP_DAT);
        return newsMapper.selectByDataKeyGtAddTime(dataKey, date, field);
    }

    /**
     * 根据多个： dataKeys 查询新闻 并且 > addTime
     *
     * @param dataKeys
     * @param field
     * @return
     */
    public List<NewsDO> listNewsDOByDataKeys(Collection<String> dataKeys, Field field) {
        Date date = DateUtil.addDate(Calendar.DAY_OF_MONTH, TOP_DAT);
        return newsMapper.selectByDataKeysGtAddTime(dataKeys, date, field);
    }

    public static void main(String[] args) {

        Date date = DateUtil.addDate(Calendar.DAY_OF_MONTH, TOP_DAT);

        System.out.println(DateUtil.format(date, "yyyy-MM-dd"));
    }
}
