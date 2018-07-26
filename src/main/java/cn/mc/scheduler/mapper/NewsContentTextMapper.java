package cn.mc.scheduler.mapper;

import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @auther sin
 * @time 2018/3/7 17:12
 */
@Mapper
@DataSource(value = DataSourceType.DB_APP)
public interface NewsContentTextMapper {

    int insert(
            @Param("update") Update update
    );
}
