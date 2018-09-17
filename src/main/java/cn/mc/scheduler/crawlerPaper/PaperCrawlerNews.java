package cn.mc.scheduler.crawlerPaper;

import cn.mc.core.dataObject.paper.PaperNewsImageDO;
import cn.mc.core.entity.BaseEntity;

import java.util.List;

/**
 * 新闻爬虫数据
 *
 * @author Sin
 * @time 2018/9/6 上午9:13
 */
public class PaperCrawlerNews extends BaseEntity {

    /**
     * dataKey
     */
    private String dataKey;
    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;
    /**
     * 摘要
     */
    private String newsAbstract;
    /**
     * 新闻 url
     */
    private String newsUrl;
    /**
     * 图片
     */
    private List<String> images;
    /**
     * 对于的 pageList
     */
    private PaperCrawlerPageList paperCrawlerPageList;

    @Override
    public String toString() {
        return "PaperCrawlerNews{" +
                "dataKey='" + dataKey + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", newsAbstract='" + newsAbstract + '\'' +
                ", newsUrl='" + newsUrl + '\'' +
                ", images=" + images +
                ", paperCrawlerPageList=" + paperCrawlerPageList +
                '}';
    }

    public String getDataKey() {
        return dataKey;
    }

    public void setDataKey(String dataKey) {
        this.dataKey = dataKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNewsAbstract() {
        return newsAbstract;
    }

    public void setNewsAbstract(String newsAbstract) {
        this.newsAbstract = newsAbstract;
    }

    public String getNewsUrl() {
        return newsUrl;
    }

    public void setNewsUrl(String newsUrl) {
        this.newsUrl = newsUrl;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public PaperCrawlerPageList getPaperCrawlerPageList() {
        return paperCrawlerPageList;
    }

    public void setPaperCrawlerPageList(PaperCrawlerPageList paperCrawlerPageList) {
        this.paperCrawlerPageList = paperCrawlerPageList;
    }
}
