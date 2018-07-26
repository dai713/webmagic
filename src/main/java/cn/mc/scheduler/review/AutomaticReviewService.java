package cn.mc.scheduler.review;

import cn.mc.core.dataObject.*;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.core.utils.BeanManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.core.utils.SensitiveWordUtil;
import cn.mc.scheduler.mapper.*;
import cn.mc.scheduler.review.rest.CheckImgClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
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
public class AutomaticReviewService {

    private static Logger logger = LoggerFactory.getLogger(AutomaticReviewService.class);

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private ReviewMapper reviewMapper;


    @Autowired
    private NewsContentArticleMapper newsContentArticleMapper;

    @Autowired
    private NewsContentPictureMapper newsContentPictureMapper;

    @Autowired
    private SchedulerLogsMapper schedulerLogsMapper;

    @Autowired
    private CheckImgClient checkImgClient;

    @Autowired
    private NewsImageMapper newsImageMapper;

    //获取配置的正则表达式
    private static  String REGXPFORTAG="<\\s*img\\s+([^>]*)\\s*";
    //获取配置的正则表达式
    private static  String REGXPFORTAGATTRIB="src=\\s*\"([^\"]+)\"";
//    private int textLength = 4000;

    /**
     * 校验图片和视频是否合格
     */
    public void reviewNewsImgVideo() {
        logger.info("====>开始执行新闻审核");
        //查询待发布的文章
        List<NewsDO> newsList = newsMapper.selectTopCommentSource(NewsDO.STATE_NOT_RELEASE,1, new Field());
        logger.info("====>需要审核的数量:"+newsList.size());

        if (CollectionUtils.isEmpty(newsList)) {
            return;
        }

        reviewArticle(newsList);
    }

    public void reviewArticle(List<NewsDO> newsList) {
        //批量处理
        for (NewsDO news : newsList) {
            //总的审核结果
            boolean reviewResult = true;
            //如果头条新闻或者娱乐newsType 图文类型或者纯文字的contentType
            if ((news.getNewsType().equals(NewsDO.NEWS_TYPE_HEADLINE)
                    || news.getNewsType().equals(NewsDO.NEWS_TYPE_ENTERTAINMENT)
                    || news.getNewsType().equals(NewsDO.NEWS_TYPE_TECHNOLOGY)
                    || news.getNewsType().equals(NewsDO.NEWS_TYPE_SPORTS))
                    && (news.getContentType().equals(NewsDO.CONTENT_TYPE_IMAGE_TEXT)
                    || news.getContentType().equals(NewsDO.CONTENT_TYPE_TEXT))) {
                //根据newsId查询相关的表的数据
                List<NewsContentArticleDO> listContentArticle = newsContentArticleMapper.selectArticleById(news.getNewsId(), new Field());
                if (!CollectionUtils.isEmpty(listContentArticle)) {

                    //查询文本里所有的图片
                    for (NewsContentArticleDO newsContentArticleDO : listContentArticle) {

                        //匹配敏感词
                        String article = newsContentArticleDO.getArticle() + "," + news.getTitle();
                        Set<String> set = SensitiveWordUtil.getSensitiveWord(article, SensitiveWordUtil.MAX_MATCH_TYPE);
                        //如果匹配到敏感词库
                        reviewResult= verificationKeyWords(reviewResult, set, news);
                        if (reviewResult == false) {
                            break;
                        }
                        //校验图片文本的公共方法
                        reviewResult = resultImgProcessing(article, news);
                        //只要检测到失败则停止处理
                        if (reviewResult == false) {
                            break;
                        }
                    }
                } else { //数据不全处理
                    reviewResult = false;
                    incompleteData(news, "newType:" + news.getNewsType() + ",contentType" + news.getContentType() + " no Article");
                }
                //提前结束当前任务
                if (reviewResult == false) {
                    continue;
                }
                //根据newsId查询视频封面的图像对象
                List<NewsImageDO> listImage = newsImageMapper.selectNewsImage(news.getNewsId(), new Field());
                if (!CollectionUtils.isEmpty(listImage)) {
                    //查询返回对象里的图片url
                    for (NewsImageDO newsImageDO : listImage) {
                        String imgUrl = newsImageDO.getImageUrl();
                        reviewResult = resultImgProcessing(imgUrl, news);
                        //只要检测到失败则停止处理
                        if (reviewResult == false) {
                            break;
                        }
                    }
                }else{//数据不全处理
                    reviewResult = false;
                    incompleteData(news, "newType:" + news.getNewsType() + ",contentType" + news.getContentType() + " no image");
                }
                //如果失败
                if (reviewResult == false) {
                    continue;
                }else {//成功
                    updateNewsStateSucces(news);
                }

            }

            //如果头条新闻或者娱乐newsType 多图类型的contentType
            if ((news.getNewsType() == NewsDO.NEWS_TYPE_HEADLINE || news.getNewsType() == NewsDO.NEWS_TYPE_ENTERTAINMENT) && news.getContentType() == NewsDO.CONTENT_TYPE_LARGE_PIC) {

                //匹配敏感词
                String article = news.getTitle();
                Set<String> set = SensitiveWordUtil.getSensitiveWord(article, SensitiveWordUtil.MAX_MATCH_TYPE);
                //如果匹配到敏感词库
                reviewResult= verificationKeyWords(reviewResult, set, news);
                if (reviewResult == false) {
                    continue;
                }
                //根据newsId查询相关类型的表的数据
                List<NewsContentPictureDO> listPicture = newsContentPictureMapper.listPicture(news.getNewsId(), new Field());
                if (listPicture.size() > 0) {
                    //查询返回对象里的图片url
                    for (NewsContentPictureDO newsContentPictureDO : listPicture) {
                        String imgUrl = newsContentPictureDO.getImageUrl();
                        //校验图片文本的公共方法
                        reviewResult = resultImgProcessing(imgUrl, news);
                        //只要检测到失败则停止处理
                        if (reviewResult == false) {
                            break;
                        }
                    }
                    //所有校验通过则更新审核状态
                    if (reviewResult) {
                        updateNewsStateSucces(news);
                    }
                } else {//数据不全处理
                    reviewResult = false;
                    incompleteData(news, "newType:" + news.getNewsType() + ",contentType" + news.getContentType() + " no picture");
                }
                if (reviewResult == false) {
                    continue;
                }
            }
            //如果段子新闻newsType 图文类型或者纯文字的contentType
            if (news.getNewsType() == NewsDO.NEWS_TYPE_SECTION && (news.getContentType() == NewsDO.CONTENT_TYPE_IMAGE_TEXT || news.getContentType() == NewsDO.CONTENT_TYPE_TEXT)) {

                //根据newsId查询相关的表的数据
                List<NewsContentArticleDO> listContentArticle = newsContentArticleMapper.selectArticleById(news.getNewsId(), new Field());
                if (listContentArticle.size() > 0) {
                    //查询文本里所有的图片
                    for (NewsContentArticleDO newsContentArticleDO : listContentArticle) {
                        String article = newsContentArticleDO.getArticle();
                        Set<String> set = SensitiveWordUtil.getSensitiveWord(article, SensitiveWordUtil.MAX_MATCH_TYPE);
                        //如果匹配到敏感词库
                        reviewResult= verificationKeyWords(reviewResult, set, news);
                        if (reviewResult == false) {
                            break;
                        }
                        //校验图片文本的公共方法
                        reviewResult = resultImgProcessing(article, news);
                        //只要检测到失败则停止处理
                        if (reviewResult == false) {
                            break;
                        }
                    }
                    if (reviewResult == false) {
                        continue;
                    }
                }
                //根据newsId查询图片对象
                List<String> listDuanzi = reviewMapper.getDuanziImg(news.getNewsId());
                //如果类型是图文的没有图片
                if (news.getContentType() == NewsDO.CONTENT_TYPE_IMAGE_TEXT && listDuanzi.size() <= 0) {
                    //数据不全
                    incompleteData(news, "newType:" + news.getNewsType() + ",contentType" + news.getContentType() + " no picture");
                    continue;

                }
                for (String duanziImage : listDuanzi) {
                    //校验图片文本的公共方法
                    reviewResult = resultImgProcessing(duanziImage, news);
                    //只要检测到失败则停止处理
                    if (reviewResult == false) {
                        break;
                    }
                }
                if (reviewResult == false) {
                    continue;
                }
                //所有校验通过则更新审核状态
                if (reviewResult) {
                    updateNewsStateSucces(news);
                }
            }

            //如果是问答类型  展示类型只有图文和纯文字类型
            if (news.getNewsType() == NewsDO.NEWS_TYPE_QA && (news.getContentType() == NewsDO.CONTENT_TYPE_IMAGE_TEXT || news.getContentType() == NewsDO.CONTENT_TYPE_TEXT)) {
                List<String> textList = reviewMapper.getQuestionAnswerText(news.getNewsId());
                //如果查询到的问题数量和新闻的问题数量不等 减1是因为问只有一条
                Integer qaCount = textList.size() - 1;
                if (qaCount != news.getQaCount()) {
                    //数据不全
                    incompleteData(news, "newType:" + news.getNewsType() + ",contentType" + news.getContentType() + " news QaCount not equal");
                    continue;
                }
                for (String text : textList) {
                    if (StringUtils.isEmpty(text)) {
                        continue;
                    }
                    Set<String> set = SensitiveWordUtil.getSensitiveWord(text, SensitiveWordUtil.MAX_MATCH_TYPE);
                    //如果匹配到敏感词库
                    reviewResult= verificationKeyWords(reviewResult, set, news);
                    if (reviewResult == false) {
                        continue;
                    }
                    //校验图片文本的公共方法
                    reviewResult = resultImgProcessing(text, news);
                    //只要检测到失败则停止处理
                    if (reviewResult == false) {
                        break;
                    }
                }
                if (reviewResult == false) {
                    continue;
                }
                //查询问答所有相关表的图片
                List<String> imgList = reviewMapper.getQuestionAnswerImg(news.getNewsId());
                for (String imgUrl : imgList) {
                    //校验图片的公共方法
                    reviewResult = resultImgProcessing(imgUrl, news);
                    //只要检测到失败则停止处理
                    if (reviewResult == false) {
                        break;
                    }
                }
                if (reviewResult == false) {
                    continue;
                }
                //所有校验通过则更新审核状态
                if (reviewResult) {
                    updateNewsStateSucces(news);
                }
            }
        }
    }

    private  void updateNewsStateSucces(NewsDO news){
        logger.info("审核通过:"+news.getNewsId());
        Update update = Update.update("news_state", NewsDO.STATE_MACHINE_AUDITS).set("display_time", DateUtil.currentDate());
        newsMapper.updateById(news.getNewsId(), update);
        //插入成功日志
        ReviewLogsDO reviewlogDO1 = new ReviewLogsDO();
        reviewlogDO1.setfId(IDUtil.getNewID());
        reviewlogDO1.setCreateTime(DateUtil.currentDate());
        reviewlogDO1.setNewsId(news.getNewsId());
        reviewlogDO1.setStatus(0);
        reviewlogDO1.setType(0);
        reviewlogDO1.setNewsType(news.getNewsType());
        schedulerLogsMapper.insertReviewLog(Update.copyWithoutNull(reviewlogDO1));
    }
    //数据不全的公共方法
    private void incompleteData(NewsDO news, String code) {
        Update update = Update.update("news_state", NewsDO.STATE_REVIEW_INCOMPLETE_DATA);
        newsMapper.updateById(news.getNewsId(), update);
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

        boolean result=checkPictureResult(content);
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
    private  boolean checkPictureResult(String content){
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
                    boolean resultCheck= checkImgClient.checkImgUrl(imgFilterUrl);
                    if(resultCheck==false){
                        return false;
                    }
                }else { //没有http则默认添加
                    String url="https:"+imgFilterUrl;
                    boolean resultCheck=  checkImgClient.checkImgUrl(url);
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
                boolean resultCheck=checkImgClient.checkImgUrl(imgFilterUrl);
                return resultCheck;
            }else{ //没有图片校验则直接返回true
                return true;
            }
        }
        return  true;
    }
    //向阿里查询视频校验结果
//    public void reviewNewsVideoResult() {
////        首先删除4个小时之前的查询视频审核结果的任务
////        reviewMapper.deleteFourHourTask();
//        //开始查询视频校验的任务
//        List<ReviewTaskVideoDO> taskList = reviewMapper.getReviewTaskVideo(new Field());
//        for (ReviewTaskVideoDO reviewTaskVideoDO : taskList) {
//            //根据任务Id获取阿里校验的结果
//            JSONObject object = aliyunGreenClientUtil.getVideoAsyncScanResults(reviewTaskVideoDO.getTaskId());
//            if (null != object) {
//                ReviewLogsDO reviewlogDO = new ReviewLogsDO();
//                reviewlogDO.setfId(IDUtil.getNewID());
//                reviewlogDO.setCreateTime(DateUtil.currentDate());
//                reviewlogDO.setNewsId(reviewTaskVideoDO.getNewsId());
//                reviewlogDO.setType(1);//视频
//                reviewlogDO.setNewsType(reviewTaskVideoDO.getNewsType());
//                //消息成功返回
//                if (object.getInteger("code") == 200) {
//                    boolean pass = true;
//                    JSONArray dataArray = object.getJSONArray("data");
//                    for (Object dataResult : dataArray) {
//                        //获取任务id
//                        String dataId = ((JSONObject) dataResult).getString("dataId");
//                        //获取url
//                        String url = ((JSONObject) dataResult).getString("url");
//                        Integer code = ((JSONObject) dataResult).getInteger("code");
//                        reviewlogDO.setResultObject(url);
//                        reviewlogDO.setResultCode(code.toString());
//                        //校验政策
//                        if (code == 200) {
//                            JSONArray resultArray = ((JSONObject) dataResult).getJSONArray("results");
//                            for (Object resultResult : resultArray) {
//                                String suggestion1 = ((JSONObject) resultResult).getString("suggestion");
//                                String scene = ((JSONObject) resultResult).getString("scene");
//                                //如果不通过
//                                if (!suggestion1.equals("pass")) {
//                                    //机器审核不通过  需要人工审核
//                                    Update update = Update.update("news_state", NewsDO.STATE_REVIEW_ARTIFICIAL);
//                                    newsMapper.updateById(Long.parseLong(dataId), update);
//                                    //修改视频审核任务表的处理状态
//                                    Update update1 = Update.update("status", 1);
//                                    reviewMapper.updateTaskVideo(reviewTaskVideoDO.gettId(), update1);
//                                    //插入系统日志
//                                    reviewlogDO.setStatus(1);
//                                    reviewlogDO.setSuggestion(suggestion1);
//                                    reviewlogDO.setScene(scene);
//                                    schedulerLogsMapper.insertReviewLog(Update.copyWithoutNull(reviewlogDO));
//                                    break;
//                                }
//                            }
//                        } else {//其他异常状态
//                            pass = false;
//                            //如果是280状态 阿里正在校验中 不做任何处理等待下次验证
//                            if (code == 280) {
//                                break;
//                            }
//                            reviewlogDO.setStatus(1);
//                            //修改视频审核任务表的处理状态
//                            Update update1 = Update.update("status", 1);
//                            reviewMapper.updateTaskVideo(reviewTaskVideoDO.gettId(), update1);
//                            schedulerLogsMapper.insertReviewLog(Update.copyWithoutNull(reviewlogDO));
//                        }
//
//                    }
//                    //校验成功
//                    if (pass == true) {
//                        Update update = Update.update("news_state", NewsDO.STATE_MACHINE_AUDITS).set("display_time", DateUtil.currentDate());
//                        newsMapper.updateById(reviewTaskVideoDO.getNewsId(), update);
//                        //修改视频审核任务表的处理状态
//                        Update update1 = Update.update("status", 1);
//                        reviewMapper.updateTaskVideo(reviewTaskVideoDO.gettId(), update1);
//                        //插入成功的日志
//                        reviewlogDO.setStatus(0);
//                        reviewlogDO.setScene("terrorism,porn");
//                        reviewlogDO.setSuggestion("pass,pass");
//                        schedulerLogsMapper.insertReviewLog(Update.copyWithoutNull(reviewlogDO));
//                    }
//                }
////
//                //查询视频审核任务表中的数量
//                int count = reviewMapper.getTaskVideoByNewsId(reviewTaskVideoDO.getNewsId(), null);
//                if (count > 2) {//如果大于2条则更新审核失败的状态
////                    //机器审核不通过
//                    Update update = Update.update("news_state", NewsDO.STATE_REVIEW_INCOMPLETE_DATA);
//                    newsMapper.updateById(reviewTaskVideoDO.getNewsId(), update);
//                }
//            }
//        }
//    }

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

    //文本过滤
//    public boolean textFilter(boolean reviewText, String text, NewsDO news) {
//        // <p>段落替换为换行
//        text = text.replaceAll("<p .*?>", "\r\n");
//        // <br><br/>替换为换行
//        text = text.replaceAll("<br\\s*/?>", "\r\n");
//        // 去掉其它的<>strObject
//        text = text.replaceAll("\\<.*?>", "");
//        //如果返回都校验失败 则更新news表状态 还有返回的错误信息
//        ReviewLogsDO reviewlogDO1 = new ReviewLogsDO();
//        reviewlogDO1.setfId(IDUtil.getNewID());
//        reviewlogDO1.setCreateTime(DateUtil.currentDate());
//        reviewlogDO1.setNewsId(news.getNewsId());
//        reviewlogDO1.setNewsType(news.getNewsType());
//        reviewlogDO1.setType(4);//文本过滤
//        StringBuffer strObject = new StringBuffer();
//        if (text.length() > 50) {
//            strObject.append(text.substring(0, 50));
//            reviewlogDO1.setResultObject(strObject.toString());
//        } else {
//            reviewlogDO1.setResultObject(text);
//        }
//        //如果内容长度超4k 分段去校验
//        if (text.length() > textLength) {
//            Double count = Double.valueOf(text.length());
//            Double count1 = count / textLength;
//            int a = (int) Math.ceil(count1) - 1;
//            for (int i = 0; i <= a; i++) {
//                if (i == 0) {
//                    JSONObject object = aliyunGreenClientUtil.textFilter(text.substring(0, textLength));
//
//                    reviewText = this.resutlTextFilter(object, reviewlogDO1, news, reviewText);
//                    if (reviewText == false) {
//                        break;
//                    }
//                } else if (i == a) {
//                    JSONObject object = aliyunGreenClientUtil.textFilter(text.substring(i * textLength, text.length()));
//                    reviewText = this.resutlTextFilter(object, reviewlogDO1, news, reviewText);
//                    if (reviewText == false) {
//                        break;
//                    }
////                        System.out.println(text.substring(i*textLength,text.length()));
//                } else {
//                    JSONObject object = aliyunGreenClientUtil.textFilter(text.substring(i * textLength, text.length()));
//                    reviewText = this.resutlTextFilter(object, reviewlogDO1, news, reviewText);
//                    if (reviewText == false) {
//                        break;
//                    }
////                        System.out.println(text.substring(i*textLength,(i+1)*textLength));
//                }
//            }
//        } else {
//            JSONObject object = aliyunGreenClientUtil.textFilter(text.substring(0, text.length()));
//            reviewText = this.resutlTextFilter(object, reviewlogDO1, news, reviewText);
//        }
//        if (reviewText == true) {
//            reviewlogDO1.setStatus(0);//成功
//            reviewlogDO1.setResultCode("200");
//            reviewlogDO1.setScene("antispam");
//            reviewlogDO1.setSuggestion("pass");
//            schedulerLogsMapper.insertReviewLog(Update.copyWithoutNull(reviewlogDO1));
//        }
//
//        return reviewText;
//    }

//    private boolean resutlTextFilter(JSONObject object, ReviewLogsDO reviewlogDO1, NewsDO news, boolean reviewText) {
//        Integer code = object.getInteger("code");
//        boolean resutl = (boolean) object.get("scuess");
//        if (code == 200) {
//            if (resutl == false) {
//                //插入审核日志
//                String scene = (String) object.get("scene");
//                String suggestion = (String) object.get("suggestion");
//                reviewlogDO1.setScene(scene);
//                reviewlogDO1.setSuggestion(suggestion);
//                reviewlogDO1.setStatus(1);//失败
//                reviewlogDO1.setResultCode(code.toString());
////                reviewlogDO1.setResultObject((String) object.get("object"));
//                schedulerLogsMapper.insertReviewLog(Update.copyWithoutNull(reviewlogDO1));
//                //机器审核不通过
//                Update update = Update.update("news_state", NewsDO.STATE_REVIEW_INCOMPLETE_DATA);
//                newsMapper.updateById(news.getNewsId(), update);
//                reviewText = false;
//            }
//        } else {
//            Update update = Update.update("news_state", NewsDO.STATE_REVIEW_INCOMPLETE_DATA);
//            newsMapper.updateById(news.getNewsId(), update);
//            reviewText = false;
//            reviewlogDO1.setStatus(1);//失败
//            reviewlogDO1.setResultCode(code.toString());
//            schedulerLogsMapper.insertReviewLog(Update.copyWithoutNull(reviewlogDO1));
//        }
//        return reviewText;
//    }

    public static void main(String[] args) {
//        AutomaticReviewService.autoVideoUrl("e1b12af1bf1e4acb8b403f617fae93cf");

    }
}
