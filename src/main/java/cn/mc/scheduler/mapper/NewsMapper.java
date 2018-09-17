package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.entity.Page;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import cn.mc.scheduler.jobs.rest.CacheClearNewsDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @auther sin
 * @time 2018/1/4 17:05
 */
@Mapper
@DataSource(value = DataSourceType.DB_APP)
public interface NewsMapper {

    int insert(
            @Param("update") Update update
    );

    int updateById(
            @Param("newsId") Long newsId,
            @Param("update") Update update
    );

    NewsDO selectById(
            @Param("newsId") Long newsId,
            @Param("fields") Field field
    );

    /**
     * 根据多个： dataKeys 查询新闻 并且 > addTime
     *
     * @param dataKeys
     * @param addTime
     * @param field
     * @return
     */
    List<NewsDO> selectByDataKeysGtAddTime(
            @Param("dataKeys") Collection<String> dataKeys,
            @Param("addTime") Date addTime,
            @Param("fields") Field field
    );

    /**
     * 根据单个： dataKeys 查询新闻 并且 > addTime
     *
     * @param dataKey
     * @param addTime
     * @param field
     * @return
     */
    NewsDO selectByDataKeyGtAddTime(
            @Param("dataKey") String dataKey,
            @Param("addTime") Date addTime,
            @Param("fields") Field field
    );

    List<NewsDO> selectListByTypeAndState(
            @Param("newsType") Integer newsType,
            @Param("newsState") Integer newsState,
            @Param("page") Page page,
            @Param("fields") Field field
    );

    /**
     * 新增新闻列表
     * @param list
     * @return
     */
    int insertNews(@Param("list") List<NewsDO> list);

    List<NewsDO> selectTopCommentSource(
            @Param("newsState") Integer newsState,@Param("newsType") Integer newsType,@Param("fields") Field field
    );

    /**
     * 新闻视频修复脚本使用
     *
     * @param newsType
     * @param newsSourceUrl
     * @param page
     * @param field
     * @return
     */
    List<NewsDO> selectByNewsTypeLikeResourceUrl(
            @Param("newsType") Integer newsType,
            @Param("newsSourceUrl") String newsSourceUrl,
            @Param("page") Page page,
            @Param("fields") Field field
    );

    List<CacheClearNewsDO> selectByDataThisWeek();

    void deleteNewsById( @Param("newsId") Long newsId);

    List<NewsDO> selectByDataToday(@Param("newsTypes") List<Integer> newsType,@Param("fields") Field field, @Param("page") Page page);
}
