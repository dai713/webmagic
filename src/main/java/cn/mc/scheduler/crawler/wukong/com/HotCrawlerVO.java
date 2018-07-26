package cn.mc.scheduler.crawler.wukong.com;

import cn.mc.core.dataObject.*;

import java.util.List;

/**
 * @auther sin
 * @time 2018/3/12 15:41
 */
public class HotCrawlerVO {

    private NewsDO newsDO;

    private NewsContentQuestionDO newsContentQuestionDO;

    private List<NewsContentQuestionImageDO> newsContentQuestionImageDOList;

    private List<NewsContentAnswerDO> newsContentAnswerDOList;

    private List<NewsQuestionAnswerUserDO> newsQuestionAnswerUserDOList;

    private List<NewsQuestionAnswerImageDO> newsQuestionAnswerImageDOList;

    @Override
    public String toString() {
        return "HotCrawlerVO{" +
                "newsDO=" + newsDO +
                ", newsContentQuestionDO=" + newsContentQuestionDO +
                ", newsContentQuestionImageDOList=" + newsContentQuestionImageDOList +
                ", newsContentAnswerDOList=" + newsContentAnswerDOList +
                ", newsQuestionAnswerUserDOList=" + newsQuestionAnswerUserDOList +
                ", newsQuestionAnswerImageDOList=" + newsQuestionAnswerImageDOList +
                '}';
    }

    public NewsDO getNewsDO() {
        return newsDO;
    }

    public void setNewsDO(NewsDO newsDO) {
        this.newsDO = newsDO;
    }

    public NewsContentQuestionDO getNewsContentQuestionDO() {
        return newsContentQuestionDO;
    }

    public void setNewsContentQuestionDO(NewsContentQuestionDO newsContentQuestionDO) {
        this.newsContentQuestionDO = newsContentQuestionDO;
    }

    public List<NewsContentQuestionImageDO> getNewsContentQuestionImageDOList() {
        return newsContentQuestionImageDOList;
    }

    public void setNewsContentQuestionImageDOList(List<NewsContentQuestionImageDO> newsContentQuestionImageDOList) {
        this.newsContentQuestionImageDOList = newsContentQuestionImageDOList;
    }

    public List<NewsContentAnswerDO> getNewsContentAnswerDOList() {
        return newsContentAnswerDOList;
    }

    public void setNewsContentAnswerDOList(List<NewsContentAnswerDO> newsContentAnswerDOList) {
        this.newsContentAnswerDOList = newsContentAnswerDOList;
    }

    public List<NewsQuestionAnswerUserDO> getNewsQuestionAnswerUserDOList() {
        return newsQuestionAnswerUserDOList;
    }

    public void setNewsQuestionAnswerUserDOList(List<NewsQuestionAnswerUserDO> newsQuestionAnswerUserDOList) {
        this.newsQuestionAnswerUserDOList = newsQuestionAnswerUserDOList;
    }

    public List<NewsQuestionAnswerImageDO> getNewsQuestionAnswerImageDOList() {
        return newsQuestionAnswerImageDOList;
    }

    public void setNewsQuestionAnswerImageDOList(List<NewsQuestionAnswerImageDO> newsQuestionAnswerImageDOList) {
        this.newsQuestionAnswerImageDOList = newsQuestionAnswerImageDOList;
    }
}
