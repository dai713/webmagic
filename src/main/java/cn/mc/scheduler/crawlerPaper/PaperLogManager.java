package cn.mc.scheduler.crawlerPaper;

import cn.mc.core.dataObject.paper.PaperLogDO;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.mapper.PaperLogMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 报纸日志 manager
 *
 * @author Sin
 * @time 2018/9/7 下午6:05
 */
@Component
public class PaperLogManager {

    @Autowired
    private PaperLogMapper paperLogMapper;

    public void savePaperLog(@NotNull Long paperId) {
//        PaperLogDO paperLogDO = new PaperLogDO();
//        paperLogDO.setId(IDUtil.getNewID());
//        paperLogDO.setPaperId(paperId);
//
//        paperLogDO.setSourceUrl();
//        paperLogDO.setContent();
//        paperLogDO.setMessage();
//        paperLogDO.setXpath();
//        paperLogDO.setMatchContent();
//        paperLogDO.setMatchPosition();
//        paperLogDO.setCreateTime(DateUtil.currentDate());
    }
}
