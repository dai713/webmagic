package cn.mc.scheduler.jobs.review;

import cn.mc.core.exception.SchedulerNewException;
import cn.mc.scheduler.base.BaseJob;
import cn.mc.scheduler.review.AutomaticReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReviewImgVideoJob extends BaseJob {

    @Autowired
    private AutomaticReviewService automaticReviewService;

    @Override
    public void execute() throws SchedulerNewException {
        automaticReviewService.reviewNewsImgVideo();
    }
}
