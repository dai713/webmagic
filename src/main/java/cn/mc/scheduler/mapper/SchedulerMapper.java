package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.scheduler.SchedulerDO;
import cn.mc.core.entity.Page;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import cn.mc.scheduler.active.query.ScheduleListQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @auther sin
 * @time 2018/2/2 18:20
 */
@Mapper
@DataSource(value = DataSourceType.DB_SCHEDULER)
public interface SchedulerMapper {

    int insert(
            @Param("update") Update update
    );

    int updateById(
            @Param("id") Long id,
            @Param("update") Update update
    );

    int updateByJobNameAndJobGroup(
            @Param("jobName") String jobName,
            @Param("jobGroup") String jobGroup,
            @Param("update") Update update
    );

    List<SchedulerDO> selectList(
            @Param("query") ScheduleListQuery query,
            @Param("page") Page page,
            @Param("fields") Field fields
    );

    Long selectListCount(
            @Param("query") ScheduleListQuery query,
            @Param("page") Page page
    );

    SchedulerDO selectByJobNameAndJobGroup(
            @Param("jobName") String jobName,
            @Param("jobGroup") String jobGroup,
            @Param("fields") Field field
    );
}
