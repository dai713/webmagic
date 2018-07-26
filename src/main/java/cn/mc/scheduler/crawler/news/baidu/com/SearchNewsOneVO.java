package cn.mc.scheduler.crawler.news.baidu.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsContentVideoDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;

import java.util.List;

/**
 * @auther sin
 * @time 2018/3/13 19:09
 */
public class SearchNewsOneVO {

    private NewsDO newsDO;

    private Integer contentType;

    private List<NewsImageDO> newsImageDOList;

    private NewsContentVideoDO newsContentVideoDO;

    private NewsContentArticleDO newsContentArticleDO;

    private NewsDisplayBO newsDisplayBO;

    @Override
    public String toString() {
        return "SearchNewsOneVO{" +
                "newsDO=" + newsDO +
                ", contentType=" + contentType +
                ", newsImageDOList=" + newsImageDOList +
                ", newsContentVideoDO=" + newsContentVideoDO +
                ", newsContentArticleDO=" + newsContentArticleDO +
                ", newsDisplayBO=" + newsDisplayBO +
                '}';
    }

    public NewsDO getNewsDO() {
        return newsDO;
    }

    public void setNewsDO(NewsDO newsDO) {
        this.newsDO = newsDO;
    }

    public Integer getContentType() {
        return contentType;
    }

    public void setContentType(Integer contentType) {
        this.contentType = contentType;
    }

    public List<NewsImageDO> getNewsImageDOList() {
        return newsImageDOList;
    }

    public void setNewsImageDOList(List<NewsImageDO> newsImageDOList) {
        this.newsImageDOList = newsImageDOList;
    }

    public NewsContentVideoDO getNewsContentVideoDO() {
        return newsContentVideoDO;
    }

    public void setNewsContentVideoDO(NewsContentVideoDO newsContentVideoDO) {
        this.newsContentVideoDO = newsContentVideoDO;
    }

    public NewsContentArticleDO getNewsContentArticleDO() {
        return newsContentArticleDO;
    }

    public void setNewsContentArticleDO(NewsContentArticleDO newsContentArticleDO) {
        this.newsContentArticleDO = newsContentArticleDO;
    }

    public NewsDisplayBO getNewsDisplayBO() {
        return newsDisplayBO;
    }

    public void setNewsDisplayBO(NewsDisplayBO newsDisplayBO) {
        this.newsDisplayBO = newsDisplayBO;
    }
}
