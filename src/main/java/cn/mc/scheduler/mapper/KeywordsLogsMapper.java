package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.logs.KeywordsLogsDO;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;

/**
 * 关键字日志
 *
 * @auther daiqingwen
 * @date 2018/6/26 下午16:39
 */
@Mapper
@DataSource(value = DataSourceType.DB_LOGS)
public interface KeywordsLogsMapper {

    /**
     * 新增关键字日志
     *
     * @param logsDO
     */
    void insert(KeywordsLogsDO logsDO);
}
