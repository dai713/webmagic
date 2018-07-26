package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.NewsContentVideoDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @auther sin
 * @time 2018/2/7 11:15
 */
@Mapper
@DataSource(value = DataSourceType.DB_APP)
public interface NewsContentVideoMapper {

    int insert(
            @Param("update") Update update
    );

    List<NewsContentVideoDO> selectContentVideoById(@Param("newsId") Long newsId,@Param("fields") Field field);

    /**
     * 新增视频
     * @param list
     */
    void insertVideo(List<NewsContentVideoDO> list);
}
