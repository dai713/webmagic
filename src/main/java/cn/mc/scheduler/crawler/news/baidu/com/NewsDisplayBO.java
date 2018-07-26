package cn.mc.scheduler.crawler.news.baidu.com;

/**
 * @auther sin
 * @time 2018/3/28 19:24
 */
public class NewsDisplayBO {

    private Integer displayType;
    private Integer contentType;

    @Override
    public String toString() {
        return "ContentTypeAndDisplayTypeBO{" +
                "displayType=" + displayType +
                ", contentType=" + contentType +
                '}';
    }

    public NewsDisplayBO(Integer displayType, Integer contentType) {
        this.displayType = displayType;
        this.contentType = contentType;
    }

    public Integer getDisplayType() {
        return displayType;
    }

    public void setDisplayType(Integer displayType) {
        this.displayType = displayType;
    }

    public Integer getContentType() {
        return contentType;
    }

    public void setContentType(Integer contentType) {
        this.contentType = contentType;
    }

}
