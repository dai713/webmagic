package cn.mc.scheduler.crawlerPaper;

import cn.mc.core.dataObject.paper.PaperNewsDO;
import cn.mc.core.dataObject.paper.PaperNewsImageDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import cn.mc.core.mysql.DynamicDataSourceContextHolder;
import cn.mc.core.utils.DateUtil;
import cn.mc.scheduler.mapper.PaperNewsImageMapper;
import cn.mc.scheduler.mapper.PaperNewsMapper;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 报纸爬虫
 *
 * @author Sin
 * @time 2018/9/4 下午5:49
 */
@Service
public class PaperPipeline {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaperPipeline.class);

    @Autowired
    private PaperNewsMapper paperNewsMapper;
    @Autowired
    private PaperNewsImageMapper paperNewsImageMapper;

//    @DataSource(value = DataSourceType.DB_PAPER)
    public void save(@NotNull PaperNewsDO paperNewsDO,
                     @NotNull List<PaperNewsImageDO> newsImages) {

        Integer status = PaperNewsDO.STATUS_NORMAL;
        Long paperId = paperNewsDO.getPaperId();

        // -30 天
        Date createTime = DateUtil.addDate(
                DateUtil.currentDate(), Calendar.DAY_OF_MONTH, -30);

        // 获取 dataKeys
        List<String> dataKeys = Lists.newArrayList(paperNewsDO.getDataKey());

        // 检查 dataKeys 是否存在
        DynamicDataSourceContextHolder.setDataSource(DataSourceType.DB_PAPER);
        List<PaperNewsDO> paperNewsDOList
                = paperNewsMapper.listByDataKeyAndGteCreateTime(
                dataKeys, paperId, status, createTime, new Field("dataKey"));

        if (!CollectionUtils.isEmpty(paperNewsDOList)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("数据库存在，不保存！dataKeys {}", dataKeys);
            }
            return;
        }

        // 去保存
        doSave(paperNewsDO, newsImages);
    }

    @Transactional
    public void doSave(@NotNull PaperNewsDO paperNewsDO,
                       @NotNull List<PaperNewsImageDO> newsImages) {
        // 开始保存
        paperNewsMapper.insert(Update.copyWithoutNull(paperNewsDO));
        // 开始保存图片
        for (PaperNewsImageDO newsImage : newsImages) {
            paperNewsImageMapper.insert(Update.copyWithoutNull(newsImage));
        }
    }
}