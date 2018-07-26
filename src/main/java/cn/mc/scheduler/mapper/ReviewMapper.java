package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.ReviewTaskVideoDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
@DataSource(value = DataSourceType.DB_APP)
public interface ReviewMapper {
    int insertReviewFail(
            @Param("update") Update update
    );

    int insertReviewTaskVideo(
            @Param("update") Update update
    );

    List<String> getQuestionAnswerImg(
            @Param("newsId") Long newsId
    );

    List<String> getQuestionAnswerText(
            @Param("newsId") Long newsId
    );

    List<String> getQuestionAnswerVideo(
            @Param("newsId") Long newsId
    );

    List<String> getDuanziImg(
            @Param("newsId") Long newsId
    );

    List<ReviewTaskVideoDO> getReviewTaskVideo(@Param("fields") Field field);

    int updateTaskVideo( @Param("tId") Long tId,@Param("update") Update update);

    int getTaskVideoByNewsId(@Param("newsId") Long newsId,@Param("status") Integer status);

    int deleteFourHourTask();

    Map<String,Object> selectKeyWords();
}
