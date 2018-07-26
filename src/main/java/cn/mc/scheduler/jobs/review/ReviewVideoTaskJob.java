package cn.mc.scheduler.jobs.review;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.review.AutomaticReviewVideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReviewVideoTaskJob extends BaseJob {

    @Autowired
    private AutomaticReviewVideoService automaticReviewVideoService;

    @Override
    public void execute() throws SchedulerNewException {
        automaticReviewVideoService.reviewNewsImgVideo();
    }
}
