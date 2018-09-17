package cn.mc.scheduler.crawlerPaper;

import cn.mc.core.entity.BaseEntity;

/**
 * page list
 *
 * @author Sin
 * @time 2018/9/6 上午9:19
 */
public class PaperCrawlerPageList extends BaseEntity {

    /**
     * 版号 (第01版)
     */
    private String pageNumber;
    /**
     * 列表 url
     */
    private String pageListUrl;
    /**
     * 列表 text (第01版：要闻)
     */
    private String pageListText;
    /**
     * 列表 title (要闻)
     */
    private String pageListTitle;

    @Override
    public String toString() {
        return "PaperCrawlerPageList{" +
                "pageNumber='" + pageNumber + '\'' +
                ", pageListUrl='" + pageListUrl + '\'' +
                ", pageListText='" + pageListText + '\'' +
                ", pageListTitle='" + pageListTitle + '\'' +
                '}';
    }

    public String getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(String pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getPageListUrl() {
        return pageListUrl;
    }

    public void setPageListUrl(String pageListUrl) {
        this.pageListUrl = pageListUrl;
    }

    public String getPageListText() {
        return pageListText;
    }

    public void setPageListText(String pageListText) {
        this.pageListText = pageListText;
    }

    public String getPageListTitle() {
        return pageListTitle;
    }

    public void setPageListTitle(String pageListTitle) {
        this.pageListTitle = pageListTitle;
    }
}
