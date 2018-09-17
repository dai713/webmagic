package cn.mc.scheduler.mapper;

import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
@DataSource(value = DataSourceType.DB_PAPER)
public interface PaperNewsImageMapper {

    /**
     * 插入 - paperNews
     *
     * @param update
     * @return
     */
    int insert(
            @Param("update") Update update
    );
}
