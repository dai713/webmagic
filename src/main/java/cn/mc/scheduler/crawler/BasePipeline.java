package cn.mc.scheduler.crawler;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

/**
 * @auther sin
 * @time 2018/3/7 14:21
 */
public abstract class BasePipeline extends CrawlerSupport implements Pipeline {

    @Override
    public void process(ResultItems resultItems, Task task) {
        if (!isNeedSave(resultItems)) {
            return;
        }

        doProcess(resultItems, task);
    }

    protected abstract void doProcess(ResultItems resultItems, Task task);
}
