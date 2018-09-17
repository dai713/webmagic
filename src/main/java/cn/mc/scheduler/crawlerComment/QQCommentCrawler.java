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
 * 抓取腾讯的详细评论
 *
 * @author xl
 * @date 2018/9/4 下午 15:46
 */
@Component
public class QQCommentCrawler extends CommentBaseCrawler{

    @Autowired
    private CommentCrawlerService commentCrawlerService;
    @Autowired
    private SaveCommentClient saveCommentClient;

    private  String commentApi="https://coral.qq.com/article/";

    private String initApi;

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);
    @Override
    public Spider createCrawler(String pushId, String product, Long newsId) {
        StringBuffer str=new StringBuffer(commentApi);
        str.append(pushId).append("/comment?commentid=0&tag=&reqnum=50&newsId=").append(newsId);
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
            Long newsId = Long.parseLong(params.get("newsId"));
            JSONObject jsonObject = JSON.parseObject(page.getRawText());
            JSONObject JSONObject1 = (JSONObject) jsonObject.get("data");
            //获取热门评论
            JSONArray commentJSONOArray = (JSONArray) JSONObject1.get("commentid");
            for(int i=0;i<commentJSONOArray.size();i++){
                //获取对象
                JSONObject jsonDataObject = (JSONObject) commentJSONOArray.get(i);
                //评论内容
                String comment=jsonDataObject.getString("content");
                if(StringUtils.isEmpty(comment)){
                    continue;
                }
                //评论时间
                String time=jsonDataObject.getString("time")+"000";
                long lt = new Long(time);
                Date date= new Date(lt);
                JSONObject userDataObject = (JSONObject)jsonDataObject.get("userinfo");
                //昵称
                String nickName=userDataObject.getString("nick");
                //昵称图像
                String headUrl=userDataObject.getString("head");
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
                //不等于空则成功 在添加评论
                if(null!=codeResult){
                    CommentSavePO commentSavePO=new CommentSavePO();
                    commentSavePO.setToken(codeResult.toString());
                    commentSavePO.setComments(comment);
                    commentSavePO.setNewsId(newsId);
                    commentSavePO.setCreateTime(date);
                    saveCommentClient.saveGrabComment(commentSavePO);
                }
            }
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
