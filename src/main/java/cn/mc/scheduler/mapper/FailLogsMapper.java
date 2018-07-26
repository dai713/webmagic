package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.logs.FailLogsDO;
import cn.mc.core.entity.Page;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @auther sin
 * @time 2018/2/3 09:06
 */
@Mapper
@DataSource(value = DataSourceType.DB_LOGS)
public interface FailLogsMapper {

    int insert(
            @Param("update") Update update
    );

    List<FailLogsDO> selectList(
            @Param("page") Page page,
            @Param("fields") Field fields
    );
}
