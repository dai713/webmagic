package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.paper.PaperNewsDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Mapper
@DataSource(value = DataSourceType.DB_PAPER)
public interface PaperNewsMapper {

    /**
     * 插入 - paperNews
     *
     * @param update
     * @return
     */
    int insert(
            @Param("update") Update update
    );

    /**
     * 更新 - 根据 id
     *
     * @param id
     * @param update
     * @return
     */
    int updateById(
            @Param("id") Long id,
            @Param("update") Update update
    );

    /**
     * 查询 list - 根据 dataKey 前 30 天
     *
     * @param dataKeys
     * @param paperId
     * @param status
     * @param field
     * @return
     */
    List<PaperNewsDO> listByDataKeyAndGteCreateTime(
            @Param("dataKeys") Collection<String> dataKeys,
            @Param("paperId") Long paperId,
            @Param("status") Integer status,
            @Param("createTime") Date createTime,
            @Param("fields") Field field
    );
}
