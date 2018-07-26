package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.entity.Page;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @auther sin
 * @time 2018/3/9 14:52
 */
@Mapper
@DataSource(value = DataSourceType.DB_APP)
public interface NewsContentArticleMapper {

    int insert(
            @Param("update") Update update
    );


    List<NewsContentArticleDO> selectPage(
            @Param("page") Page page,
            @Param("fields") Field field
    );

    int updateById(
            @Param("id") Long id,
            @Param("update") Update update
    );

    /**
     * 新增文章内容
     * @param articleDo
     * @return
     */
    int insertArticle(NewsContentArticleDO articleDo);

    List<NewsContentArticleDO> selectArticleById(
            @Param("newsId") Long newsId,@Param("fields") Field field
    );

    /**
     * 新增文章列表
     * @param list
     * @return
     */
    int insertArticleList(List<NewsContentArticleDO> list);
}
