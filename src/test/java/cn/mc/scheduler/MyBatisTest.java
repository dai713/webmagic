package cn.mc.scheduler;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.scheduler.mapper.NewsMapper;
import cn.mc.scheduler.mapper.SchedulerMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

/**
 * @auther sin
 * @time 2018/2/1 17:21
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class MyBatisTest {

    private Logger logger = LoggerFactory.getLogger(MyBatisTest.class);

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private SchedulerMapper schedulerMapper;

    @Test
    public void updateTest() {
        Long newsId = 976647217106386944L;
        String updateTitle = "update title!";

        int updateResult = newsMapper.updateById
                (newsId, Update.update("title", updateTitle));

        Assert.isTrue(updateResult > 0,
                "更新的数据小于 0 ！");

        NewsDO newsDO = newsMapper.selectById(newsId, new Field());
        Assert.isTrue(updateTitle.equals(newsDO.getTitle()),
                "title 更新失败!");

        System.out.println(newsDO);
    }

//    @Test
//    public void schedulerTest() {
////        DynamicDataSourceContextHolder.setDataSource(DataSourceType.DB_DEFAULT);
////        DynamicDataSourceContextHolder.clearDataSource();
//        schedulerMapper.selectListCount(new Page());
//        List<SchedulerDO> schedulerDOList = schedulerMapper.selectList(new Page(), new Field());
//        for (SchedulerDO schedulerDO : schedulerDOList) {
//            System.out.println(schedulerDO);
//        }
//    }
//
//    @Test
//    public void schedulerInsertTest() {
//        schedulerMapper.insert(
//                Update.update("id", IDUtil.getNewID())
//                        .set("jobName", "jobN")
//                        .set("jobGroup", "group").set("schedName", "name")
//        );
//    }
//
//    @Test
//    public void newsInsertEmoji() {
//        NewsDO newsDO = new NewsDO();
//        newsDO.setNewsId(IDUtil.getNewTestId());
//        newsDO.setTitle("哈哈😆sad😆😆😆😆😆😆公共🙄🙄🙄🙄🙄🙄hah");
//        newsMapper.insert(Update.copyWithoutNull(newsDO));
//    }
}
