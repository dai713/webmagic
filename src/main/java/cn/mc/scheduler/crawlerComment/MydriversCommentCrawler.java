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
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;

import java.util.*;

/**
 * 快科技的评论抓取
 *
 * @author xl
 * @date 2018/9/4 下午 15:46
 */
@Component
public class MydriversCommentCrawler extends CommentBaseCrawler{
    @Autowired
    private CommentCrawlerService commentCrawlerService;
    @Autowired
    private SaveCommentClient saveCommentClient;

    private  String commentApi="http://comment8.mydrivers.com/ReviewAjax.aspx?Tid=";

    private  String headUrl="http://passport.mydrivers.com/comments/getusertouxiang.aspx?uid=";

    private String initApi;

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);
    @Override
    public Spider createCrawler(String pushId, String product, Long newsId) {
        StringBuffer str=new StringBuffer(commentApi);
        str.append(pushId).append("&Cid=1&Page=1&hot=0&timestamp=").append(System.currentTimeMillis());
        str.append("&newsId=").append(newsId);
        initApi=str.toString();
        Request request = new Request(initApi);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public void process(Page page) {
            Map<String, String> params = analyticalUrlParams(page.getUrl().toString());
            if (CollectionUtils.isEmpty(params)) {
                return;
            }
            Integer pageNow = Integer.parseInt(params.get("Page"));
            //driver的tid
            String tid = params.get("Tid");
            //自己新闻的newsId
            Long newsId = Long.parseLong(params.get("newsId"));
            //如果分页小于等于第3页
            if(pageNow<=3){
                String jsonReturn = page.getRawText();
                JSONObject jsonObject = JSON.parseObject(jsonReturn);
                JSONArray dataJSONOArray = (JSONArray) jsonObject.get("All");
                if(dataJSONOArray.size()<=0){
                    return;
                }
                for (int i = 0; i < dataJSONOArray.size(); i++) {
                    //获取对象
                    JSONObject jsonDataObject = (JSONObject) dataJSONOArray.get(i);
                    //封装需要添加的抓取的用户
                    String nickName= String.valueOf(jsonDataObject.get("UserName"));
                    String uid= String.valueOf(jsonDataObject.get("UserID"));
                    String headUrl=null;
                    if(!StringUtils.isEmpty(uid)){
                        headUrl=getHeaderLocal(uid);
                        if(!headUrl.equals("/images/photo.jpg")){
                            headUrl="http://passport.mydrivers.com/"+headUrl;
                        }else {
                            headUrl=null;
                        }
                    }
                    //评论时间
                    String time=jsonDataObject.getString("PostDate");
                    time=time.replaceAll("/","-");
                    Date publishTime = DateUtil.parse(time,"yyyy-MM-dd HH:mm:ss");
                    //评论内容
                    String comment=String.valueOf(jsonDataObject.get("RevertContent"));
                    if(StringUtils.isEmpty(comment)){
                        comment=String.valueOf(jsonDataObject.get("Content"));
                    }
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
                    StringBuffer apiStr=new StringBuffer(commentApi);
                    pageNow++;
                    apiStr.append(tid).append("&Cid=1&Page=").append(pageNow).append("&hot=0&timestamp=").append(System.currentTimeMillis());
                    apiStr.append("&newsId=").append(newsId);
                    String requestUrl=apiStr.toString();
                    Request request = new Request(requestUrl);
                    page.addTargetRequest(request);
                }
            }

    }

    @Override
    public Site getSite() {
        return site;
    }
    private String getHeaderLocal(String uid){
        String url=headUrl+uid+"&size=medium";
//        String url="http://passport.mydrivers.com/comments/getusertouxiang.aspx?uid=";
        HttpPost get;
        HttpResponse response;
        HttpClient client = new DefaultHttpClient();
        try {
            get=new HttpPost(url);
            response = client.execute(get);
            Header header=response.getFirstHeader("Location");
            return header.getValue();
//            System.out.println(header.getValue());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
