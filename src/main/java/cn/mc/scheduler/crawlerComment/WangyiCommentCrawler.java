package cn.mc.scheduler.crawlerComment;

import cn.mc.core.dataObject.GrabUserDO;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawlerComment.rest.CommentSavePO;
import cn.mc.scheduler.crawlerComment.rest.SaveCommentClient;
import cn.mc.scheduler.util.RedisUtil;
import com.alibaba.fastjson.JSON;
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
 * 抓取网易的详细评论
 *
 * @author xl
 * @date 2018/9/4 下午 15:46
 */
@Component
public class WangyiCommentCrawler extends CommentBaseCrawler{
    @Autowired
    private CommentCrawlerService commentCrawlerService;
    @Autowired
    private SaveCommentClient saveCommentClient;
    @Autowired
    private RedisUtil redisUtil;

    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);

    private  String commentApi="https://comment.news.163.com/api/v1/products/a2869674571f77b5a0867c3d71db5856/threads/";

    private String initApi;
    @Override
    public Spider createCrawler(String pushId, String product, Long newsId) {
        StringBuffer str=new StringBuffer(commentApi);
        str.append(pushId).append("/comments/newList?offset=0&limit=30&headLimit=3&tailLimit=2&ibc=newswap&newsId=").append(newsId).append("&pushId=").append(pushId);
        initApi=str.toString();
        Request request = new Request(initApi);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public void process(Page page) {
            if(StringUtils.isEmpty(page.getRawText())){
                return;
            }
            Map<String, String> params = analyticalUrlParams(page.getUrl().toString());
            if (CollectionUtils.isEmpty(params)) {
                return;
            }
            //自己新闻的newsId
            Long newsId = Long.parseLong(params.get("newsId"));
            //页码偏移数
            int offset = Integer.parseInt(params.get("offset"));
            //网易参数
            String pushId = params.get("pushId");
            String redisWangyiCountKey=newsId+"_wangyiCommentCount";
            //从缓存中获取已经抓取保存的数量
            Integer addCacheCount=0;
            String addCacheCount1= redisUtil.getString(redisWangyiCountKey);
            if(null!=addCacheCount1){
                addCacheCount=Integer.parseInt(addCacheCount1.toString());
            }
            //大于50条 结束抓取
            if(addCacheCount>50){
                return;
            }
            JSONObject jsonObject = JSON.parseObject(page.getRawText());
            JSONObject jsonObject1= (JSONObject) jsonObject.get("comments");
            if(jsonObject1.size()<=0){
                return;
            }
            //统计本次符合条件存到库里的数据
            int addCount=0;
            for (Map.Entry<String, Object> entry : jsonObject1.entrySet()) {
                JSONObject jsonObject2= (JSONObject) jsonObject1.get(entry.getKey());
                Integer level=jsonObject2.getInteger("buildLevel");
                //只获取一级评论
                if(level!=1){
                   continue;
                }
                //评论内容
                String comment=jsonObject2.getString("content");
                //评论时间
                String time=jsonObject2.getString("createTime");
                Date commentTime = DateUtil.parse(time,"yyyy-MM-dd HH:mm:ss");
                JSONObject userDataObject = (JSONObject)jsonObject2.get("user");
                //昵称
                String nickName=userDataObject.getString("nickname");


                if(StringUtils.isEmpty(nickName)){
                  continue;
                }
                //昵称图像
                String headUrl=userDataObject.getString("avatar");
                if(StringUtils.isEmpty(headUrl)){
                    continue;
                }
                //如果是网易的默认头像则为null
                if(headUrl.equals("http://cms-bucket.nosdn.127.net/2018/08/13/078ea9f65d954410b62a52ac773875a1.jpeg")){
                    headUrl=null;
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
                    commentSavePO.setCreateTime(commentTime);
                    boolean resultSave=saveCommentClient.saveGrabComment(commentSavePO);
                    if(resultSave==false){
                        addCount++;
                    }
                }
            }
            addCacheCount=addCacheCount+addCount;
            //更新缓存
            redisUtil.setString(redisWangyiCountKey,addCacheCount.toString(),60);
            if(addCacheCount<50){
                //继续抓取详情页面
                StringBuffer apiStr=new StringBuffer(commentApi);
                offset=offset+30;
                apiStr.append(pushId).append("/comments/newList?offset=").append(offset).append("&limit=30&headLimit=3&tailLimit=2&ibc=newswap&newsId=").append(newsId).append("&pushId=").append(pushId);
                String requestUrl=apiStr.toString();
                Request request = new Request(requestUrl);
                page.addTargetRequest(request);
            }

    }

    @Override
    public Site getSite() {
        return site;
    }
}
