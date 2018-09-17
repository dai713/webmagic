package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.NewsContentPictureDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @auther sin
 * @time 2018/3/29 11:12
 */
@Mapper
@DataSource(value = DataSourceType.DB_APP)
public interface NewsContentPictureMapper {

    int insert(@Param("update") Update update);

    /**
     * 新增大图新闻
     * @param pictureDO
     * @return int
     */
    int insertPicture(NewsContentPictureDO pictureDO);

    /**
     * 查询图片列
     * @param newsId
     * @return List<NewsContentPictureDO>
     */
    List<NewsContentPictureDO> listPicture(@Param("newsId") Long newsId,@Param("fields") Field field);

    /**
     * 新增大图新闻
     * @param pictureList
     * @return int
     */
    void insertPictureList(List<NewsContentPictureDO> pictureList);
}
