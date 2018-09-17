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
public interface PaperMapper {

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
     * 更新 - 根据 pageName
     *
     * @param pageName
     * @param update
     * @return
     */
    int updateByPageName(
            @Param("pageName") String pageName,
            @Param("update") Update update
    );

    /**
     * 获取 list - 根据 page
     *
     * @param status
     * @param field
     * @return
     */
    List<PaperDO> listPaperPage(
            @Param("page") Page page,
            @Param("status") Integer status,
            @Param("fields") Field field
    );

    /**
     * 获取 count
     *
     * @param status
     * @return
     */
    int getPaperCount(
            @Param("status") Integer status
    );


    /**
     * 获取 last paper
     *
     * @param field
     * @return
     */
    PaperDO getLastPaper(
            @Param("fields") Field field
    );
}
