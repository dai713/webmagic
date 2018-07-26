package cn.mc.scheduler.jobs.stats;

import cn.mc.core.dataObject.work.ReviewLogsStatsDO;
import cn.mc.core.dataObject.work.StatsNewsCreateHourDO;
import cn.mc.core.exception.SchedulerNewException;
import cn.mc.core.utils.DateUtil;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.mapper.ReviewLogsMapper;
import cn.mc.scheduler.mapper.StatsNewsCreateHourMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class StatsNewsCreateHour extends BaseJob {

    @Autowired
    private ReviewLogsMapper reviewLogsMapper;

    @Autowired
    private StatsNewsCreateHourMapper statsNewsCreateHourMapper;

    /**
     *  每隔一小时执行根据日志统计新闻机器审核数据
     */
    @Override
    public void execute() throws SchedulerNewException {
        work();
    }

    public void work() {
        try{
            System.out.println("每隔一小时执行根据日志统计新闻机器审核数据");
            String day = DateUtil.operDay(0);
            String hour = DateUtil.getPreviousHour();
            if("23".equals(hour)){//上一个小时为23即上一小时为上一天
                day = DateUtil.operDay(-1);
            }
            List<ReviewLogsStatsDO> list =  reviewLogsMapper.selectByReviewList(1, day, hour);
            for (ReviewLogsStatsDO reviewLogsDO :
                    list) {
                String createDay = reviewLogsDO.getCreateDay();
                int newsType = reviewLogsDO.getNewsType();
                int type = reviewLogsDO.getType();
                int status = reviewLogsDO.getStatus();
                int count = reviewLogsDO.getCount();

                StatsNewsCreateHourDO statsNewsCreateHourDO = new StatsNewsCreateHourDO();
                if(type == 0){//图片
                    if(status == 0){//成功
                        statsNewsCreateHourDO.setImageSuccessCount(count);
                    }else if(status == 1){//失败
                        statsNewsCreateHourDO.setImageErrorCount(count);
                    }
                }else if(type == 1){//视频
                    if(status == 0){//成功
                        statsNewsCreateHourDO.setVideoSuccessCount(count);
                    }else if(status == 1){//失败
                        statsNewsCreateHourDO.setVideoErrorCount(count);
                    }
                }else if(type == 2){//关键字
                    if(status == 0){//成功
                        statsNewsCreateHourDO.setKeywordSuccessCount(count);
                    }else if(status == 1){//失败
                        statsNewsCreateHourDO.setKeywordErrorCount(count);
                    }
                }else if(type == 3){//错别字
                    if(status == 0){//成功
                        statsNewsCreateHourDO.setWrongSuccessCount(count);
                    }else if(status == 1){//失败
                        statsNewsCreateHourDO.setWrongErrorCount(count);
                    }
                }
                System.out.println("机器审核按小时统计：" + statsNewsCreateHourDO.toString());
                statsNewsCreateHourDO.setCreateDay(createDay);
                statsNewsCreateHourDO.setHour(Integer.parseInt(hour));
                statsNewsCreateHourDO.setNewsType(newsType);
                statsNewsCreateHourMapper.updateById(createDay, statsNewsCreateHourDO);
            }

            /**
             * 统计总数
             */
            List<ReviewLogsStatsDO> totallist =  reviewLogsMapper.selectByReviewStatsTotal(1, day, hour);
            for (ReviewLogsStatsDO reviewLogsDO :
                    totallist) {
                String createDay = reviewLogsDO.getCreateDay();
                int newsType = reviewLogsDO.getNewsType();
                int status = reviewLogsDO.getStatus();
                int count = reviewLogsDO.getCount();

                StatsNewsCreateHourDO statsNewsCreateHourDO = new StatsNewsCreateHourDO();

                if(status == 0){//成功总数
                    statsNewsCreateHourDO.setSuccessCount(count);
                }else if(status == 1){//失败总数
                    statsNewsCreateHourDO.setErrorCount(count);
                }
                System.out.println("机器审核按小时总数统计：" + statsNewsCreateHourDO.toString());
                statsNewsCreateHourDO.setCreateDay(createDay);
                statsNewsCreateHourDO.setHour(Integer.parseInt(hour));
                statsNewsCreateHourDO.setNewsType(newsType);
                statsNewsCreateHourMapper.updateById(createDay, statsNewsCreateHourDO);
            }

            /**
             * 统计暴恐总数 terrorism暴恐
             */
            List<ReviewLogsStatsDO> terrorismlist =  reviewLogsMapper.selectByReviewScene(1, day, hour, "terrorism");
            for (ReviewLogsStatsDO reviewLogsDO :
                    terrorismlist) {
                String createDay = reviewLogsDO.getCreateDay();
                int newsType = reviewLogsDO.getNewsType();
                int status = reviewLogsDO.getStatus();
                int count = reviewLogsDO.getCount();

                StatsNewsCreateHourDO statsNewsCreateHourDO = new StatsNewsCreateHourDO();

                if(status == 0){//成功总数
                    statsNewsCreateHourDO.setTerrorismSuccessCount(count);
                }else if(status == 1){//失败总数
                    statsNewsCreateHourDO.setTerrorismErrorCount(count);
                }
                System.out.println("机器审核按小时暴恐总数统计：" + statsNewsCreateHourDO.toString());
                statsNewsCreateHourDO.setCreateDay(createDay);
                statsNewsCreateHourDO.setHour(Integer.parseInt(hour));
                statsNewsCreateHourDO.setNewsType(newsType);
                statsNewsCreateHourMapper.updateById(createDay, statsNewsCreateHourDO);
            }

            /**
             * 统计暴恐总数 porn涉黄
             */
            List<ReviewLogsStatsDO> pornlist =  reviewLogsMapper.selectByReviewScene(1, day, hour, "porn");
            for (ReviewLogsStatsDO reviewLogsDO :
                    pornlist) {
                String createDay = reviewLogsDO.getCreateDay();
                int newsType = reviewLogsDO.getNewsType();
                int status = reviewLogsDO.getStatus();
                int count = reviewLogsDO.getCount();

                StatsNewsCreateHourDO statsNewsCreateHourDO = new StatsNewsCreateHourDO();

                if(status == 0){//成功总数
                    statsNewsCreateHourDO.setPornSuccessCount(count);
                }else if(status == 1){//失败总数
                    statsNewsCreateHourDO.setPornErrorCount(count);
                }
                System.out.println("机器审核按小时涉黄总数统计：" + statsNewsCreateHourDO.toString());
                statsNewsCreateHourDO.setCreateDay(createDay);
                statsNewsCreateHourDO.setHour(Integer.parseInt(hour));
                statsNewsCreateHourDO.setNewsType(newsType);
                statsNewsCreateHourMapper.updateById(createDay, statsNewsCreateHourDO);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
