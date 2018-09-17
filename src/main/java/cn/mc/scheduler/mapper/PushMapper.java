package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.PushInfoDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@DataSource(value = DataSourceType.DB_APP)
public interface PushMapper {

    List<PushInfoDO> selectTimePush(@Param("fields") Field field);

    int updatePushStatus(@Param("pushId") Long pushId);
}
