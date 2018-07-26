package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.work.ReviewLogsStatsDO;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @auther lijian
 */
@Mapper
@DataSource(value = DataSourceType.DB_LOGS)
public interface ReviewLogsMapper {

    List<ReviewLogsStatsDO> selectByReviewList(
            @Param("type") int type,
            @Param("createDay") String createDay,
            @Param("hour") String hour
    );

    List<ReviewLogsStatsDO> selectByReviewStatsTotal(
            @Param("type") int type,
            @Param("createDay") String createDay,
            @Param("hour") String hour
    );

    /**
     * 统计暴恐、涉黄
     * @param type
     * @param createDay
     * @param hour
     * @param scene terrorism暴恐,porn涉黄
     * @return
     */
    List<ReviewLogsStatsDO> selectByReviewScene(
            @Param("type") int type,
            @Param("createDay") String createDay,
            @Param("hour") String hour,
            @Param("scene") String scene
    );
}
