package cn.mc.scheduler.script.news;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.entity.Page;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.scheduler.mapper.NewsContentArticleMapper;
import cn.mc.scheduler.script.BaseScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 修复 - 新闻内容 img 标签
 *
 * @auther sin
 * @time 2018/3/27 09:46
 */
@Component
public class RepairNewsContentImgLabelScript extends BaseScript {

    @Autowired
    private NewsContentArticleMapper newsContentArticleMapper;

    @Override
    public void script() {
        int index = 0;

        Page page = new Page();
        page.setSize(100);
        page.setIndex(index);
        int newsType = 0;
        rRn(page, 0);
    }

    private void rRn(Page page, int newsType) {
        List<NewsContentArticleDO> newsContentAnswerDOList
                = newsContentArticleMapper.selectPage(page, new Field());

        for (NewsContentArticleDO newsContentArticleDO : newsContentAnswerDOList) {
            String article = newsContentArticleDO.getArticle();
            if (StringUtils.isEmpty(article))
                continue;

            article = article.replaceAll("<imgsrc", "<img src");
            newsContentArticleMapper.updateById(
                    newsContentArticleDO.getNewsContentArticleId(),
                    Update.update("article", article));
        }

        if (newsContentAnswerDOList.size() >= page.getSize()) {
            page.setIndex(page.getIndex() + 1);
            rRn(page, newsType);
        }
    }
}
