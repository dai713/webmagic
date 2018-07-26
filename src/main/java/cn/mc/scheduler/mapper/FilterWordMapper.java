package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.work.FilterWordDO;
import cn.mc.core.entity.Page;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author sin
 * @time 2018/6/26 10:32
 */
@Mapper
@DataSource(value = DataSourceType.DB_ADMIN)
public interface FilterWordMapper {


    /**
     * 列表分页
     *
     * @param page
     * @param status
     * @param field
     * @return
     */
    List<FilterWordDO> listLimit(
            @Param("page") Page page,
            @Param("status") Integer status,
            @Param("fields") Field field
    );

    /**
     * 查询 all
     *
     * @param status
     * @param field
     * @return
     */
    List<FilterWordDO> listAll(
            @Param("status") Integer status,
            @Param("fields") Field field
    );
}
