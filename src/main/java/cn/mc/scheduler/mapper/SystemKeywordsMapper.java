package cn.mc.scheduler.mapper;

import cn.mc.core.dataObject.SystemKeywordsDO;
import cn.mc.core.mysql.DataSource;
import cn.mc.core.mysql.DataSourceType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 系统关键字dao
 *
 * @auther daiqingwen
 * @date 2018-8-20 上午10:29
 */

@Mapper
@DataSource(value = DataSourceType.DB_ADMIN)
public interface SystemKeywordsMapper {

    /**
     * 查询需要跳过的关键字
     *
     * @return SystemKeywordsDO
     */
    List<SystemKeywordsDO> queryNeedJump();

    /**
     * 查询需要替换的关键字
     *
     * @return
     */
    List<SystemKeywordsDO> queryNeedReplace();
}
