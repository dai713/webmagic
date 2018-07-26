package cn.mc.scheduler.crawler.toutiao.com;

import cn.mc.core.dataObject.NewsContentVideoDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;

import java.util.List;

/**
 * @auther sin
 * @time 2018/3/15 18:31
 */
public class VideoCrawlerVO {

    private NewsDO newsDO;

    private List<NewsImageDO> newsImageDOList;

    private NewsContentVideoDO newsContentVideoDO;

    @Override
    public String toString() {
        return "VideoCrawlerVO{" +
                "newsDO=" + newsDO +
                ", newsImageDOList=" + newsImageDOList +
                ", newsContentVideoDO=" + newsContentVideoDO +
                '}';
    }

    public NewsDO getNewsDO() {
        return newsDO;
    }

    public void setNewsDO(NewsDO newsDO) {
        this.newsDO = newsDO;
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
}
