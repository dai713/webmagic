package cn.mc.scheduler.crawlerComment;

import cn.mc.core.dataObject.GrabUserDO;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawlerComment.rest.CommentSavePO;
import cn.mc.scheduler.crawlerComment.rest.SaveCommentClient;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;

import java.util.Date;
import java.util.Map;

/**
 * 抓取新浪详细评论
 *
 * @author xl
 * @date 2018/8/29 下午 15:46
 */
@Component
public class SinaCommentCrawler  extends CommentBaseCrawler {

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private  String commentApi="https://cmnt.sina.cn/aj/v2/list?";

    private String commentHref="https://cmnt.sina.cn/index?product=";
    @Autowired
    private SaveCommentClient saveCommentClient;

    private  String initUrl;

    @Autowired
    private CommentCrawlerService commentCrawlerService;

    @Override
    synchronized public  Spider createCrawler(String pushId, String product, Long newsId) {
        //如果产品类型为空 则默认comos
        if(StringUtils.isEmpty(product)){
            product="comos";
        }
        StringBuffer commentUrl=new StringBuffer(commentHref);
        commentUrl.append(product).append("&index=").append(pushId).append("&tj_ch=mil&is_clear=1&wm=&").append("&newsId=").append(newsId);
        initUrl=commentUrl.toString();
        Request request = new Request(initUrl);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }
    @Override
    public void  process(Page page) {
        if (initUrl.contains(page.getUrl().toString())) {

            Map<String, String> params = analyticalUrlParams(page.getUrl().toString());
            if (CollectionUtils.isEmpty(params)) {
                return;
            }
            String newsId = params.get("newsId");
            Html html = page.getHtml();
            //渠道
            String channel=html.xpath("//input[@name='channel']/@value").nodes().get(0).toString();
            //sina的newsID
            String newsid=html.xpath("//input[@name='newsid']/@value").nodes().get(0).toString();
            //sina的group
            String group=html.xpath("//input[@name='group']/@value").nodes().get(0).toString();
            StringBuffer apiStr=new StringBuffer();
            Long timeMillis=System.currentTimeMillis()/1000;
            apiStr.append(commentApi).append("channel=").append(channel).append("&newsid=").append(newsid).append("&group=").append(group).append("&thread=1&page=1&_=").append(timeMillis).append("&_callback=jsonp1");
            String requestUrl=apiStr.toString();
            //继续抓取详情页面
            Request request = new Request(requestUrl + "&newsId=" + newsId);
            request=addRequest(request);
            page.addTargetRequest(request);
        }else{ //则是返回接口的评论
            Map<String, String> params = analyticalUrlParams(page.getUrl().toString());
            if (CollectionUtils.isEmpty(params)) {
                return;
            }
            Integer pageNow = Integer.parseInt(params.get("page"));
            //sina的newsid
            String newsid = params.get("newsid");
            //自己新闻的newsId
            Long newsId = Long.parseLong(params.get("newsId"));
            String channel =params.get("channel");
            String group =params.get("group");
            //如果分页小于等于第3页
            if(pageNow<=3){
                String jsonReturn = page.getRawText();
                String jsonStr = jsonReturn.replaceAll("jsonp"+pageNow, "");
                jsonStr = jsonStr.substring(jsonStr.indexOf("(") + 1, jsonStr.lastIndexOf(")"));
                JSONObject jsonObject = JSON.parseObject(jsonStr);
                JSONObject jsonObject1 = (JSONObject) jsonObject.get("result");
                JSONArray dataJSONOArray = (JSONArray) jsonObject1.get("cmntlist");
                if(dataJSONOArray.size()<=0){
                    return;
                }
                for (int i = 0; i < dataJSONOArray.size(); i++) {
                    //获取对象
                    JSONObject jsonDataObject = (JSONObject) dataJSONOArray.get(i);
                    //封装需要添加的抓取的用户
                    String nickName= String.valueOf(jsonDataObject.get("nick"));
                    //头像
                    String headUrl=String.valueOf(jsonDataObject.get("face"));
                    //评论时间
                    Date publishTime = DateUtil.parse(String.valueOf(jsonDataObject.get("time")),"yyyy-MM-dd HH:mm:ss");
                    //评论内容
                    String comment=String.valueOf(jsonDataObject.get("content"));
                    if(StringUtils.isEmpty(comment)){
                        continue;
                    }
                    GrabUserDO grabUserDO=new GrabUserDO();
                    grabUserDO.setGrabId(IDUtil.getNewID());
                    grabUserDO.setNickName(nickName);
                    grabUserDO.setHeadUrl(headUrl);
                    grabUserDO.setCreateTime(DateUtil.currentDate());
                    //优先保存抓取的用户信息
                    Long codeResult=commentCrawlerService.addCommentUser(grabUserDO);
                    //不等于空则成功
                    if(null!=codeResult){
                        CommentSavePO commentSavePO=new CommentSavePO();
                        commentSavePO.setToken(codeResult.toString());
                        commentSavePO.setComments(comment);
                        commentSavePO.setNewsId(newsId);
                        commentSavePO.setCreateTime(publishTime);
                        saveCommentClient.saveGrabComment(commentSavePO);
                    }
                }
                //最后一页的问题 只取3页
                if(pageNow<3){
                    //继续抓取详情页面
                    StringBuffer apiStr=new StringBuffer();
                    Long timeMillis=System.currentTimeMillis()/1000;
                    pageNow++;
                    apiStr.append(commentApi).append("channel=").append(channel).append("&newsid=").append(newsid).append("&group=").append(group).append("&thread=1&page=").append(pageNow).append("&_=").append(timeMillis).append("&_callback=jsonp").append(pageNow);
                    String requestUrl=apiStr.toString();
                    Request request = new Request(requestUrl + "&newsId=" + newsId);
                    request=addRequest(request);
                    page.addTargetRequest(request);
                }

            }else {
                return;
            }
        }
    }

   private  Request addRequest(Request request){
       request.setMethod("GET");
       request.addHeader("Accept", "*/*");
       request.addHeader("Accept-Encoding", "gzip, deflate, br");
       request.addHeader("Accept-Language", "zh-CN,zh;q=0.9");
       request.addHeader("Cache-Control", "no-cache");
       request.addHeader("Connection", "keep-alive");
       request.addHeader("Host", "cmnt.sina.cn");
       request.addHeader("Pragma", "no-cache");
       request.addHeader("Referer", commentHref);
       request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
       return  request;
   }
    @Override
    public Site getSite() {
        return site;
    }

}
