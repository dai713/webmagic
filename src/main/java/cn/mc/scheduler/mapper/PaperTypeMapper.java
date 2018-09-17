package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.paper.PaperTypeDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@DataSource(value = DataSourceType.DB_PAPER)
public interface PaperTypeMapper {

    /**
     * 插入 - paperType
     *
     * @param update
     * @return
     */
    int insert(
            @Param("update") Update update
    );

    /**
     * 查询 - all
     *
     * @param fields
     * @return
     */
    List<PaperTypeDO> listAll(
            @Param("status") Integer status,
            @Param("fields") Field fields
    );
}
