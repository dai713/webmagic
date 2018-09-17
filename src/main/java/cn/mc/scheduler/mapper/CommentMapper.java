package cn.mc.scheduler.mapper;


import cn.mc.core.dataObject.GrabUserDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @auther xl
 * @time 2018/8/31 09:06
 */
@Mapper
@DataSource(value = DataSourceType.DB_COMMENT)
public interface CommentMapper {

    int insertGrabUser(@Param("update") Update update);

    List<GrabUserDO> getGrabUser(@Param("nickName") String nickName, @Param("fields") Field field);

    int getNewsCommentCount(@Param("cTable") String cTable,@Param("newsId") Long newsId);
}
