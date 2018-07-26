package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.NewsQuestionAnswerImageDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * @auther sin
 * @time 2018/3/12 15:21
 */
@Mapper
@DataSource(value = DataSourceType.DB_APP)
public interface NewsQuestionAnswerImageMapper {

    int insert(
            @Param("update") Update update
    );

    List<NewsQuestionAnswerImageDO> selectByDataKeys(
            @Param("dataKeys") Set<String> dataKeys,
            @Param("fields") Field field
    );
}
