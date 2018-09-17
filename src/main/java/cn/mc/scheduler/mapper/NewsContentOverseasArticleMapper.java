package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.NewsContentOverseasArticleDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 新闻 - 海外文章
 *
 * @auther sin
 * @time 2018/3/9 14:52
 */
@Mapper
@DataSource(value = DataSourceType.DB_APP)
public interface NewsContentOverseasArticleMapper {

    /**
     * 插入
     *
     * @param update
     * @return
     */
    int insert(
            @Param("update") Update update
    );

    /**
     * 根据 id 更新
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
     * 获取 NewsContentOverseasArticleDO 根据 newsId
     *
     * @param newsContentOverseasArticleId
     * @param field
     * @return
     */
    NewsContentOverseasArticleDO getByNewsId(
            @Param("newsContentOverseasArticleId") Long newsContentOverseasArticleId,
            @Param("fields") Field field
    );
}
