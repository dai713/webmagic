package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.paper.PaperDO;
import cn.mc.core.entity.Page;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@DataSource(value = DataSourceType.DB_PAPER)
public interface PaperLogMapper {

    /**
     * 插入 - paperLog
     *
     * @param update
     * @return
     */
    int insert(
            @Param("update") Update update
    );
}
