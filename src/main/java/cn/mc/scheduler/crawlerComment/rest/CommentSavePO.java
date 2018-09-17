package cn.mc.scheduler.crawlerComment.rest;

import cn.mc.core.entity.BaseEntity;
import java.util.Date;
import javax.validation.constraints.NotNull;

/**
 * 评论保存
 *
 * @auther sin
 * @time 2018/3/27 13:35
 */
public class CommentSavePO extends BaseEntity {
    /**
     * 会话标识
     */
    @NotNull
    private String token;
    /**
     * 评论内容
     */
    private String comments;
    /**
     * 新闻源id
     */
    @NotNull
    private Long newsId;
    /**
     * 评论时间
     */
    private Date createTime;

    @Override
    public String toString() {
        return "CommentSavePO{" +
                "token='" + token + '\'' +
                ", comments='" + comments + '\'' +
                ", newsId=" + newsId +
                ", createTime=" + createTime +
                '}';
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getNewsId() {
        return newsId;
    }

    public void setNewsId(Long newsId) {
        this.newsId = newsId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }


}
