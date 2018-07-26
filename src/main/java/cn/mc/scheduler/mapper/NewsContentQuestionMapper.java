package cn.mc.scheduler.mapper;

import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @auther sin
 * @time 2018/3/22 11:23
 */
@Mapper
@DataSource(value = DataSourceType.DB_APP)
public interface NewsContentQuestionMapper {

    int insert(
            @Param("update") Update update
    );
}
