package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.work.StatsNewsCreateDayDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @auther lijian
 */
@Mapper
@DataSource(value = DataSourceType.DB_ADMIN)
public interface StatsNewsCreateDayMapper {

    int updateById(
            @Param("createDay") String createDay,
            @Param("statsNews") StatsNewsCreateDayDO statsNewsCreateDayDO
    );

}
