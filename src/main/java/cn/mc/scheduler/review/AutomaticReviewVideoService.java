package cn.mc.scheduler.review;

import cn.mc.core.dataObject.*;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.service.AuthVideoService;
import cn.mc.core.utils.BeanManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.core.utils.SensitiveWordUtil;
import cn.mc.scheduler.mapper.*;
import cn.mc.scheduler.review.rest.CheckImgClient;
import cn.mc.scheduler.review.rest.CheckVideoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @auther xl
 * @time 2018/4/26 17:30
 */
@Component
public class AutomaticReviewVideoService {

    private static Logger logger = LoggerFactory.getLogger(AutomaticReviewVideoService.class);

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private NewsImageMapper newsImageMapper;

    @Autowired
    private NewsContentVideoMapper newsContentVideoMapper;

    @Autowired
    private AuthVideoService authVideoService;

    @Autowired
    private SchedulerLogsMapper schedulerLogsMapper;

    @Autowired
    private CheckVideoClient checkVideoClient;
    @Autowired
    private CheckImgClient checkImgClient;


    //获取配置的正则表达式
    private static  String REGXPFORTAG="<\\s*img\\s+([^>]*)\\s*";
    //获取配置的正则表达式
    private static  String REGXPFORTAGATTRIB="src=\\s*\"([^\"]+)\"";

    /**
     * 校验图片和视频是否合格
     */
    public void reviewNewsImgVideo() {
        logger.info("====>开始执行视频审核");
        //查询待发布的文章
        List<NewsDO> newsList = newsMapper.selectTopCommentSource(NewsDO.STATE_NOT_RELEASE, 2,new Field());
        logger.info("====>需要视频审核的数量:"+newsList.size());

    }

    public void reviewVideo(List<NewsDO> newsList) {
        //批量处理
        for (NewsDO news : newsList) {
            //总的审核结果
            boolean reviewResult = true;

            //如果头条新闻或者娱乐newsType 视频类型的contentType
            if ((news.getNewsType() == NewsDO.NEWS_TYPE_HEADLINE || news.getNewsType() == NewsDO.NEWS_TYPE_ENTERTAINMENT) && news.getContentType() == NewsDO.CONTENT_TYPE_VIDEO) {
                //匹配敏感词
                String article = news.getTitle();
                Set<String> set = SensitiveWordUtil.getSensitiveWord(article, SensitiveWordUtil.MAX_MATCH_TYPE);
                //如果匹配到敏感词库
                reviewResult= verificationKeyWords(reviewResult, set, news);
                if (reviewResult == false) {
                    continue;
                }
                //处理视频类的方法
                processingVideo(news);
            }

            //如果视频新闻newsType 只有视频类型的contentType
            if (news.getNewsType() == NewsDO.NEWS_TYPE_VIDEO && news.getContentType() == NewsDO.CONTENT_TYPE_VIDEO) {
                //匹配敏感词
                String article = news.getTitle();
                Set<String> set = SensitiveWordUtil.getSensitiveWord(article, SensitiveWordUtil.MAX_MATCH_TYPE);
                //如果匹配到敏感词库
                reviewResult= verificationKeyWords(reviewResult, set, news);
                if (reviewResult == false) {
                    continue;
                }
                //处理视频类的方法
                processingVideo(news);
            }
        }
    }

    //处理视频类型的方法 可共用
    private void processingVideo(NewsDO news) {
        //根据newsId查询视频封面的图像对象
        List<NewsImageDO> listImage = newsImageMapper.selectNewsImage(news.getNewsId(), new Field());
        //总的审核结果
        boolean reviewResult = true;
        if (listImage.size() > 0) {
            //查询返回对象里的图片url
            for (NewsImageDO newsImageDO : listImage) {
                String imgUrl = newsImageDO.getImageUrl();
                reviewResult = resultImgProcessing(imgUrl, news);
                //只要检测到失败则停止处理
                if (reviewResult == false) {
                    break;
                }

            }
        } else {//没有视频封面
            reviewResult = false;
            //数据不全处理
            incompleteData(news, "newsType:" + news.getNewsType() + ",newsContentType:" + news.getContentType() + ",no image");
        }
        //如果成功
        if (reviewResult == true) {
            //根据newsId查询视频对象
            List<NewsContentVideoDO> listVideo = newsContentVideoMapper.selectContentVideoById(news.getNewsId(), new Field());
            for (NewsContentVideoDO contentVideo : listVideo) {
                String url=contentVideo.getFileUrl();
                //如果需要授权
                if(contentVideo.getAuthAccess()==1){
                    //根据datakey获取鉴权的视频url
                    url = authVideoService.authVideoService(contentVideo.getDataKey());
                }
                try {
                    reviewResult= checkVideoClient.checkVideoUrl(url,news.getNewsId());
                }catch (Exception ex){
                    ex.printStackTrace();
                    reviewResult=false;
                }

                ReviewLogsDO reviewlogDO = new ReviewLogsDO();
                reviewlogDO.setfId(IDUtil.getNewID());
                reviewlogDO.setCreateTime(DateUtil.currentDate());
                reviewlogDO.setNewsId(news.getNewsId());
                reviewlogDO.setType(1);//视频
                reviewlogDO.setNewsType(news.getNewsType());
                //失败
                if(reviewResult==false){
                    System.out.println("视频审核不通过:"+news.getAddTime()+","+news.getNewsId());
                    //机器审核不通过  需要人工审核
                    Update update = Update.update("news_state", NewsDO.STATE_REVIEW_ARTIFICIAL);
                    newsMapper.updateById(news.getNewsId(), update);
                    //插入系统日志
                    reviewlogDO.setStatus(1);
                    schedulerLogsMapper.insertReviewLog(Update.copyWithoutNull(reviewlogDO));
                }else {
                    System.out.println("视频审核通过:"+news.getAddTime()+","+news.getNewsId());
                    //成功
                    Update update = Update.update("news_state", NewsDO.STATE_MACHINE_AUDITS).set("display_time", DateUtil.currentDate());
                    newsMapper.updateById(news.getNewsId(), update);
                    //插入成功的日志
                    reviewlogDO.setStatus(0);
                    schedulerLogsMapper.insertReviewLog(Update.copyWithoutNull(reviewlogDO));
                }
                //结束 视频是一对一的
                break;
            }
        }
    }

    //数据不全的公共方法
    private void incompleteData(NewsDO news, String code) {
        Update update = Update.update("news_state", NewsDO.STATE_REVIEW_INCOMPLETE_DATA);
        newsMapper.updateById(news.getNewsId(), update);
        SchedulerLogsMapper schedulerLogsMapper = BeanManager.getBean(SchedulerLogsMapper.class);
        ReviewLogsDO reviewlogDO2 = new ReviewLogsDO();
        reviewlogDO2.setfId(IDUtil.getNewID());
        reviewlogDO2.setCreateTime(DateUtil.currentDate());
        reviewlogDO2.setNewsId(news.getNewsId());
        reviewlogDO2.setNewsType(news.getNewsType());
        reviewlogDO2.setStatus(3);//数据不全  //机器审核不通过
        reviewlogDO2.setResultCode(code);
        schedulerLogsMapper.insertReviewLog(Update.copyWithoutNull(reviewlogDO2));

    }

    //图片结果处理的公共方法
    private boolean resultImgProcessing(String content, NewsDO news) {

        boolean result=checkPictureResult(content,news.getNewsId());
        //如果返回都校验失败 则更新news表状态 还有返回的错误信息
        ReviewLogsDO reviewlogDO1 = new ReviewLogsDO();
        reviewlogDO1.setfId(IDUtil.getNewID());
        reviewlogDO1.setCreateTime(DateUtil.currentDate());
        reviewlogDO1.setNewsId(news.getNewsId());
        reviewlogDO1.setNewsType(news.getNewsType());
        if (content.length() > 50) {
            content = content.substring(0, 50);
            reviewlogDO1.setResultObject(content);
        } else {
            reviewlogDO1.setResultObject(content);
        }
        reviewlogDO1.setType(0);//图片
        if (result == false) {
            //插入审核日志
            reviewlogDO1.setStatus(1);//失败
            reviewlogDO1.setResultCode("200");
            schedulerLogsMapper.insertReviewLog(Update.copyWithoutNull(reviewlogDO1));
            //机器审核不通过
            Update update = Update.update("news_state", NewsDO.STATE_REVIEW_INCOMPLETE_DATA);
            newsMapper.updateById(news.getNewsId(), update);
        }
        return result;

    }
    private  boolean checkPictureResult(String content,Long newsId){
        Pattern patternForTag = Pattern.compile (REGXPFORTAG,Pattern. CASE_INSENSITIVE );
        Pattern patternForAttrib = Pattern.compile (REGXPFORTAGATTRIB,Pattern. CASE_INSENSITIVE );
        Matcher matcherForTag = patternForTag.matcher(content);
        StringBuffer sb = new StringBuffer();
        boolean result = matcherForTag.find();
        while (result) {
            StringBuffer sbreplace = new StringBuffer( "<img ");
            Matcher matcherForAttrib = patternForAttrib.matcher(matcherForTag.group(1));
            if (matcherForAttrib.find()) {
                String attributeStr = matcherForAttrib.group(1);
                String imgFilterUrl=new String(attributeStr.replaceAll("amp;","").trim());
                if (imgFilterUrl.startsWith("http:") || imgFilterUrl.startsWith("https:")) {
                    boolean resultCheck= checkImgClient.checkImgUrl(imgFilterUrl,newsId);
                    if(resultCheck==false){
                        return false;
                    }
                }else { //没有http则默认添加
                    String url="https:"+imgFilterUrl;
                    boolean resultCheck=  checkImgClient.checkImgUrl(url,newsId);
                    if(resultCheck==false){
                        return false;
                    }
                }
            }
            matcherForAttrib.appendTail(sbreplace);
            matcherForTag.appendReplacement(sb, sbreplace.toString());
            result = matcherForTag.find();
        }
        //如果找不到img则是直接的图片url
        if(result==false){
            String imgFilterUrl=new String(content.replaceAll("amp;","").trim());
            if (imgFilterUrl.startsWith("http:") || imgFilterUrl.startsWith("https:")) {
                boolean resultCheck=checkImgClient.checkImgUrl(imgFilterUrl,newsId);
                return resultCheck;
            }else{ //没有图片校验则直接返回true
                return true;
            }
        }
        return  true;
    }

    //校验关键字过滤
    public boolean verificationKeyWords(boolean reviewResult, Set<String> set, NewsDO news) {
        ReviewLogsDO reviewlogDO = new ReviewLogsDO();
        reviewlogDO.setfId(IDUtil.getNewID());
        reviewlogDO.setCreateTime(DateUtil.currentDate());
        reviewlogDO.setNewsId(news.getNewsId());
        reviewlogDO.setNewsType(news.getNewsType());
        reviewlogDO.setType(2);//关键字
        if (set.size()>0) {

            reviewlogDO.setStatus(1);//失败
            reviewlogDO.setResultObject(set.toString());
            //机器审核不通过
            Update update = Update.update("news_state", 5);
            newsMapper.updateById(news.getNewsId(), update);
            reviewResult = false;
        } else {
            reviewlogDO.setStatus(0);//成功
        }
        schedulerLogsMapper.insertReviewLog(Update.copyWithoutNull(reviewlogDO));
        return reviewResult;
    }

}
