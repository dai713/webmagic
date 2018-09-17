package cn.mc.scheduler.crawlerOverseas;

import cn.mc.core.dataObject.paper.PaperTypeDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.scheduler.SchedulerApplication;
import cn.mc.scheduler.mapper.PaperMapper;
import cn.mc.scheduler.mapper.PaperTypeMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * @author Sin
 * @time 2018/9/15 上午10:23
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class PaperConvertTest {

    @Autowired
    private PaperTypeMapper paperTypeMapper;
    @Autowired
    private PaperMapper paperMapper;

    @Test
    public void convertTest() {
        List<PaperTypeDO> paperTypeDOList = paperTypeMapper
                .listAll(PaperTypeDO.STATUS_NORMAL, new Field());

        for (PaperTypeDO paperTypeDO : paperTypeDOList) {
            paperMapper.updateByPageName(
                    paperTypeDO.getPaperName(),
                    Update.update("paperNumber", paperTypeDO.getPaperNumber())
            );
        }
    }
}
