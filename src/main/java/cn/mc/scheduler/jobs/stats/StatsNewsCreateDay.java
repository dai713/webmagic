package cn.mc.scheduler.jobs.stats;

import cn.mc.core.dataObject.work.ReviewLogsStatsDO;
import cn.mc.core.dataObject.work.StatsNewsCreateDayDO;
import cn.mc.core.exception.SchedulerNewException;
import cn.mc.core.utils.DateUtil;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.mapper.StatsNewsCreateDayMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import cn.mc.scheduler.mapper.ReviewLogsMapper;
import java.util.List;

@Component
public class StatsNewsCreateDay extends BaseJob {

    @Autowired
    private ReviewLogsMapper reviewLogsMapper;

    @Autowired
    private StatsNewsCreateDayMapper statsNewsCreateDayMapper;

    /**
     *  每天2点执行根据日志统计新闻机器审核数据
     */
    @Override
    public void execute() throws SchedulerNewException {
        work();
    }

    public void work() {
        try{
            System.out.println("每天2点执行根据日志统计新闻机器审核数据");
            String day = DateUtil.operDay(-1);
            List<ReviewLogsStatsDO> list =  reviewLogsMapper.selectByReviewList(0, day, "-1");
            for (ReviewLogsStatsDO reviewLogsDO :
                    list) {
                String createDay = reviewLogsDO.getCreateDay();
                int newsType = reviewLogsDO.getNewsType();
                int type = reviewLogsDO.getType();
                int status = reviewLogsDO.getStatus();
                int count = reviewLogsDO.getCount();

                StatsNewsCreateDayDO statsNewsCreateDayDO = new StatsNewsCreateDayDO();
                if(type == 0){//图片
                    if(status == 0){//成功
                        statsNewsCreateDayDO.setImageSuccessCount(count);
                    }else if(status == 1){//失败
                        statsNewsCreateDayDO.setImageErrorCount(count);
                    }
                }else if(type == 1){//视频
                    if(status == 0){//成功
                        statsNewsCreateDayDO.setVideoSuccessCount(count);
                    }else if(status == 1){//失败
                        statsNewsCreateDayDO.setVideoErrorCount(count);
                    }
                }else if(type == 2){//关键字
                    if(status == 0){//成功
                        statsNewsCreateDayDO.setKeywordSuccessCount(count);
                    }else if(status == 1){//失败
                        statsNewsCreateDayDO.setKeywordErrorCount(count);
                    }
                }else if(type == 3){//错别字
                    if(status == 0){//成功
                        statsNewsCreateDayDO.setWrongSuccessCount(count);
                    }else if(status == 1){//失败
                        statsNewsCreateDayDO.setWrongErrorCount(count);
                    }
                }
                System.out.println("机器审核按天统计：" + statsNewsCreateDayDO.toString());
                statsNewsCreateDayDO.setCreateDay(createDay);
                statsNewsCreateDayDO.setNewsType(newsType);
                statsNewsCreateDayMapper.updateById(createDay, statsNewsCreateDayDO);
            }

            /**
             * 统计总数
             */
            List<ReviewLogsStatsDO> totallist =  reviewLogsMapper.selectByReviewStatsTotal(0, day, "-1");
            for (ReviewLogsStatsDO reviewLogsDO :
                    totallist) {
                String createDay = reviewLogsDO.getCreateDay();
                int newsType = reviewLogsDO.getNewsType();
                int status = reviewLogsDO.getStatus();
                int count = reviewLogsDO.getCount();

                StatsNewsCreateDayDO statsNewsCreateDayDO = new StatsNewsCreateDayDO();
                if(status == 0){//成功总数
                    statsNewsCreateDayDO.setSuccessCount(count);
                }else if(status == 1){//失败总数
                    statsNewsCreateDayDO.setErrorCount(count);
                }
                System.out.println("机器审核按天总数统计：" + statsNewsCreateDayDO.toString());
                statsNewsCreateDayDO.setCreateDay(createDay);
                statsNewsCreateDayDO.setNewsType(newsType);
                statsNewsCreateDayMapper.updateById(createDay, statsNewsCreateDayDO);
            }

            /**
             * 统计暴恐总数 terrorism暴恐
             */
            List<ReviewLogsStatsDO> terrorismlist =  reviewLogsMapper.selectByReviewScene(0, day, "-1", "terrorism");
            for (ReviewLogsStatsDO reviewLogsDO :
                    terrorismlist) {
                String createDay = reviewLogsDO.getCreateDay();
                int newsType = reviewLogsDO.getNewsType();
                int status = reviewLogsDO.getStatus();
                int count = reviewLogsDO.getCount();

                StatsNewsCreateDayDO statsNewsCreateDayDO = new StatsNewsCreateDayDO();
                if(status == 0){//成功总数
                    statsNewsCreateDayDO.setTerrorismSuccessCount(count);
                }else if(status == 1){//失败总数
                    statsNewsCreateDayDO.setTerrorismErrorCount(count);
                }
                System.out.println("机器审核按天暴恐总数统计：" + statsNewsCreateDayDO.toString());
                statsNewsCreateDayDO.setCreateDay(createDay);
                statsNewsCreateDayDO.setNewsType(newsType);
                statsNewsCreateDayMapper.updateById(createDay, statsNewsCreateDayDO);
            }

            /**
             * 统计涉黄总数 porn涉黄
             */
            List<ReviewLogsStatsDO> pornlist =  reviewLogsMapper.selectByReviewScene(0, day, "-1", "porn");
            for (ReviewLogsStatsDO reviewLogsDO :
                    pornlist) {
                String createDay = reviewLogsDO.getCreateDay();
                int newsType = reviewLogsDO.getNewsType();
                int status = reviewLogsDO.getStatus();
                int count = reviewLogsDO.getCount();

                StatsNewsCreateDayDO statsNewsCreateDayDO = new StatsNewsCreateDayDO();
                if(status == 0){//成功总数
                    statsNewsCreateDayDO.setPornSuccessCount(count);
                }else if(status == 1){//失败总数
                    statsNewsCreateDayDO.setPornErrorCount(count);
                }
                System.out.println("机器审核按天涉黄总数统计：" + statsNewsCreateDayDO.toString());
                statsNewsCreateDayDO.setCreateDay(createDay);
                statsNewsCreateDayDO.setNewsType(newsType);
                statsNewsCreateDayMapper.updateById(createDay, statsNewsCreateDayDO);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
