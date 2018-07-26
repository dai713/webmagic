package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.NewsSectionUserDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @auther sin
 * @time 2018/3/15 09:47
 */
@Mapper
@DataSource(value = DataSourceType.DB_APP)
public interface NewsSectionUserMapper {

    int insert(
            @Param("update") Update update
    );

    NewsSectionUserDO selectByDataKey(
            @Param("dataKey") String dataKey,
            @Param("fields") Field field
    );

    List<NewsSectionUserDO> selectUserDataById(
            @Param("newsId") Long newsId
    );
}
