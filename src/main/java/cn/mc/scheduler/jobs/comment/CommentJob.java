package cn.mc.scheduler.jobs.comment;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.entity.Page;
import cn.mc.core.exception.SchedulerNewException;
import cn.mc.core.mybatis.Field;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.crawlerComment.CommentBase1Crawler;
import cn.mc.scheduler.mapper.NewsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CommentJob  extends BaseJob {
    @Autowired
    private NewsMapper newsMapper;
    @Autowired
    private CommentBase1Crawler commentBase1Crawler;
    @Override
    public void execute() throws SchedulerNewException {
        //首先查询出所有的今天抓取的审核通过的新闻
        List<Integer> newsTypes=new ArrayList<>();
        //头条
        newsTypes.add(0);newsTypes.add(1);newsTypes.add(7); newsTypes.add(8);newsTypes.add(9);newsTypes.add(10);
        for(int i=0;i<3000;i++){
            Page page = new Page();
            page.setIndex(i);
            page.setSize(80);
            List<NewsDO> newsList=newsMapper.selectByDataToday(newsTypes,new Field(),page);
            if(newsList.size()>0){
                commentBase1Crawler.createCrawler(newsList).thread(1).run();
            }else{
                break;
            }
        }
    }
}
