package cn.mc.scheduler.jobs.rest;

import cn.mc.core.entity.BaseEntity;

/**
 * 缓存 - 清除 news
 *
 * @auther sin
 * @time 2018/4/19 19:57
 */
public class CacheClearNewsPO extends BaseEntity {

    /**
     * 需要清除的 news
     */
    private Long newsId;
    /**
     * 新闻类型
     */
    private Integer newsType;

    @Override
    public String toString() {
        return "CacheClearNewsPO{" +
                "newsId=" + newsId +
                ", newsType=" + newsType +
                '}';
    }

    public Long getNewsId() {
        return newsId;
    }

    public void setNewsId(Long newsId) {
        this.newsId = newsId;
    }

    public Integer getNewsType() {
        return newsType;
    }

    public void setNewsType(Integer newsType) {
        this.newsType = newsType;
    }
}
