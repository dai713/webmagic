package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.work.StatsNewsCreateHourDO;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @auther lijian
 */
@Mapper
@DataSource(value = DataSourceType.DB_ADMIN)
public interface StatsNewsCreateHourMapper {

    int updateById(
            @Param("createDay") String createDay,
            @Param("statsNews") StatsNewsCreateHourDO statsNewsCreateHourDO
            );

}
