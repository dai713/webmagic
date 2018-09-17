package cn.mc.scheduler.crawlerPaper;

import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Spider;

import java.util.List;
import java.util.Map;

/**
 * 报纸
 *
 * @author Sin
 * @time 2018/9/4 下午5:51
 */
public interface PaperInvoking {

    Spider invoke(List<Request> requestList, Map<String, Object> params);
}
