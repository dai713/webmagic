//package cn.mc.scheduler.crawlerPaper;
//
//import cn.mc.core.dataObject.paper.PaperDO;
//import cn.mc.core.mybatis.Field;
//import cn.mc.core.utils.Http2Util;
//import cn.mc.scheduler.mapper.GrabPaperLogMapper;
//import cn.mc.scheduler.mapper.PaperMapper;
//import cn.mc.scheduler.mapper.PaperNewsMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.util.CollectionUtils;
//import org.springframework.util.StringUtils;
//import us.codecraft.webmagic.selector.Html;
//
//import java.util.List;
//
///**
// * 报纸爬虫
// *
// * @author Sin
// * @time 2018/9/4 下午5:49
// */
//@Component
//public class PaperCrawler2 {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(PaperCrawler2.class);
//
//    @Autowired
//    private PaperMapper paperMapper;
//    @Autowired
//    private PaperNewsMapper paperNewsMapper;
//    @Autowired
//    private GrabPaperLogMapper grabPaperLogMapper;
//
//    private void list() {
//        List<PaperDO> paperDOList = paperMapper.selectAllList(new Field());
//        if (CollectionUtils.isEmpty(paperDOList)) {
//            return;
//        }
//
//        for (PaperDO paperDO : paperDOList) {
//            String url = paperDO.getUrl();
//            String resultHtml = request(url);
//
//            if (StringUtils.isEmpty(resultHtml)) {
//                if (LOGGER.isErrorEnabled()) {
//                    LOGGER.error("未抓取到内容! url {} resultHtml {}", url, resultHtml);
//                }
//                continue;
//            }
//
//            // 转换 html node，用于 xpath 查找
//            Html htmlNode = Html.create(resultHtml);
//
//            // 获取 matcher 表达式（此处为 xpath)
//            String pageMatcher = paperDO.getPageMatcher();
//            String titleMatcher = paperDO.getTitleMatcher();
//            String contentMatcher = paperDO.getContentMatcher();
//
//
//        }
//    }
//
//    private String request(String url) {
//        return Http2Util.httpGet(url, null);
//    }
//}