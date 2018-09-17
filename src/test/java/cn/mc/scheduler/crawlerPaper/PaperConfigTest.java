package cn.mc.scheduler.crawlerPaper;

import cn.mc.core.dataObject.paper.PaperDO;
import cn.mc.core.dataObject.paper.PaperTypeDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.SchedulerApplication;
import cn.mc.scheduler.mapper.PaperMapper;
import cn.mc.scheduler.mapper.PaperTypeMapper;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 报纸配置
 *
 * @author Sin
 * @time 2018/9/7 下午5:16
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class PaperConfigTest {

    @Autowired
    private PaperMapper paperMapper;
    @Autowired
    private PaperTypeMapper paperTypeMapper;
    @Autowired
    private PaperCrawler paperCrawler;

    @Test
    public void configPaperTypeTest() {
        String paperName = "深圳都市报";

        // 全国 000000
        // 上海 310000
        // 北京 110000
        // 天津 120000
        // 中山市 440000
        // 河南省 410000
        String provinceNumber = "000000";

        // 中山市 442000
        String cityNumber = "0";

        // 省报 PAPER_TYPE_PROVINCE_NEWS_PAPER
        // 市报 PAPER_TYPE_CITY_NEWS_PAPER
        // 县报 PAPER_TYPE_COUNTY_NEWS_PAPER
        // 国报 PAPER_TYPE_COUNTRY_NEWS_PAPER
        Integer paperType = PaperTypeDO.PAPER_TYPE_COUNTRY_NEWS_PAPER;

        PaperTypeDO paperTypeDO = new PaperTypeDO();
        paperTypeDO.setId(IDUtil.getNewID());
        paperTypeDO.setPaperName(paperName);
        paperTypeDO.setCityNumber(cityNumber);
        paperTypeDO.setProvinceNumber(provinceNumber);
        paperTypeDO.setPaperType(paperType);

        PaperDO basePaperDO = paperMapper.getLastPaper(new Field());
        Integer newPaperNumber = basePaperDO.getPaperNumber() + 1;
        paperTypeDO.setPaperNumber(newPaperNumber);

        PaperDO paperDO = new PaperDO();
        paperDO.setId(IDUtil.getNewID());

        paperDO.setPaperNumber(newPaperNumber);
        paperDO.setPageName(paperName);

        // {date,yyyy-MM}/{date,dd}
        paperDO.setPageUrl("https://dtzbd.sznews.com/PC/layout/{date,yyyyMM}/{date,dd}/node_A01.html");


        // 版面列表
        paperDO.setPageListXpath("//div[@class='Chunkiconlist']/p/a[1]");

        // 新闻列表
        paperDO.setNewsListXpath("//div[@class='newslist']/ul/li/h3/a");

        // 新闻标题
        paperDO.setNewsTitleXpath("//div[@class='newsdetatit']/h3");

        // 新闻内容
        paperDO.setContentXpath("//div[@class='newsdetatext']");


        paperDO.setMemo("");
        paperDO.setCreateTime(DateUtil.currentDate());
        paperDO.setStatus(PaperDO.STATUS_NORMAL);
        paperMapper.insert(Update.copyWithoutNull(paperDO));
        paperTypeMapper.insert(Update.copyWithoutNull(paperTypeDO));

        // start
        paperCrawler.start(Lists.newArrayList(paperDO));
    }
}
