package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @auther sin
 * @time 2018/2/5 11:09
 */
@Mapper
@DataSource(value = DataSourceType.DB_APP)
public interface NewsImageMapper {

    int insert(@Param("update") Update update);

    /**
     * 新增新闻列表图片
     * @param list
     * @return
     */
    int insertNewsImage(List<NewsImageDO> list);

    /**
     * 新增新闻列表图片
     * @param newsId
     * @return
     */
    List<NewsImageDO> selectNewsImage(@Param("newsId") Long newsId,@Param("fields") Field field);

}
