package cn.mc.scheduler.jobs.rest;

import cn.mc.core.entity.BaseEntity;

public class CacheClearNewsDO extends BaseEntity {
    /**
     * 需要清除的 news
     */
    private Long newsId;
    /**
     * 新闻类型
     */
    private Integer newsType;

    /**
     * url 地址
     */
    private String imageUrl;

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
