package cn.mc.scheduler.mapper;

import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @auther sin
 * @time 2018/2/3 09:06
 */
@Mapper
@DataSource(value = DataSourceType.DB_LOGS)
public interface SchedulerLogsMapper {

    int insert(
            @Param("update") Update update
    );

    int insertReviewLog(
            @Param("update") Update update
    );

    int insertReviewFailLog(
            @Param("update") Update update
    );
}
