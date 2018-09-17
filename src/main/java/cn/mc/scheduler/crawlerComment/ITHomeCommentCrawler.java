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
import java.util.Date;
import java.util.Map;

/**
 * 抓取it之家详细评论
 *
 * @author xl
 * @date 2018/9/4 下午 15:46
 */
@Component
public class ITHomeCommentCrawler  extends CommentBaseCrawler {

    @Autowired
    private CommentCrawlerService commentCrawlerService;
    @Autowired
    private SaveCommentClient saveCommentClient;

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private  String commentApi="https://m.ithome.com/api/comment/newscommentlistget?NewsID=";

    private String initApi;
    @Override
    public Spider createCrawler(String pushId, String product, Long newsId) {
        StringBuffer str=new StringBuffer(commentApi);
        //因为我们的newsId和it之家的命名一样 会查询不出 所以我们零时改变参数名
        str.append(pushId).append("&LapinID=&MaxCommentID=0&Latest=&").append("newsId1=").append(newsId);
        initApi=str.toString();
        Request request = new Request(initApi);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public void process(Page page) {
        //接口返回的评论
        if (initApi.contains(page.getUrl().toString())) {
            if(StringUtils.isEmpty(page.getRawText())){
                return;
            }
            Map<String, String> params = analyticalUrlParams(page.getUrl().toString());
            if (CollectionUtils.isEmpty(params)) {
                return;
            }
            //自己新闻的newsId
            Long newsId = Long.parseLong(params.get("newsId1"));
            JSONObject jsonObject = JSON.parseObject(page.getRawText());
            JSONObject JSONObject1 = (JSONObject) jsonObject.get("Result");

            //获取热门评论
            JSONArray hiJSONOArray = (JSONArray) JSONObject1.get("Hlist");
            //新增的评论数量
            int addCount = 50;
            if(hiJSONOArray.size()>0){
                //添加业务处理共用方法
                addComment(hiJSONOArray,newsId,addCount);
            }
            //如果热门评论数不到50 则继续解析普通评论
            if( hiJSONOArray.size()<50){
                if(hiJSONOArray.size()>0) {
                    addCount = 50 - addCount;
                }
                //获取普通评论
                JSONArray cJSONOArray = (JSONArray) JSONObject1.get("Clist");
                if(cJSONOArray.size()>0) {
                    addComment(cJSONOArray,newsId,addCount);
                }
            }
        }
    }
    private void addComment(JSONArray jSONOArray,Long newsId,int addCount){
        for(int i=0;i<jSONOArray.size();i++){
            if(addCount==i){
                return;
            }
            //获取对象
            JSONObject jsonDataObject = (JSONObject) jSONOArray.get(i);
            JSONObject object = (JSONObject) jsonDataObject.get("M");
            //评论内容
            String comment=object.getString("C");
            if(StringUtils.isEmpty(comment)){
                continue;
            }
            //昵称
            String nickName=object.getString("N");
            //昵称图像
            String headUrl=object.getString("HeadImg");
            //评论时间
            String time=object.getString("T").replaceAll("T"," ");
            //发布时间
            Date displayTime = null;
            try {
                displayTime= DateUtil.parse(time,DateUtil.DATE_FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND2);;
            }catch (Exception ex){
                ex.printStackTrace();
            }
            String regex = "<span>.*</span>";
            comment=comment.replaceAll(regex,"");
            GrabUserDO grabUserDO=new GrabUserDO();
            grabUserDO.setGrabId(IDUtil.getNewID());
            grabUserDO.setNickName(nickName);
            grabUserDO.setHeadUrl(headUrl);
            grabUserDO.setCreateTime(DateUtil.currentDate());
            //优先保存抓取的用户信息
            Long codeResult=commentCrawlerService.addCommentUser(grabUserDO);
            //不等于空则成功 在添加评论
            if(null!=codeResult){
                CommentSavePO commentSavePO=new CommentSavePO();
                commentSavePO.setToken(codeResult.toString());
                commentSavePO.setComments(comment);
                commentSavePO.setNewsId(newsId);
                commentSavePO.setCreateTime(displayTime);
                saveCommentClient.saveGrabComment(commentSavePO);
            }
        }
    }
    @Override
    public Site getSite() {
        return site;
    }
}
