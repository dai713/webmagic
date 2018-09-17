package cn.mc.scheduler.crawlerComment;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.utils.TableUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.mapper.CommentMapper;
import cn.mc.scheduler.mapper.NewsMapper;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 总的抓取评论任务
 *
 * @author xl
 * @date 2018/8/29 下午 15:46
 */
@Component
public class CommentCrawler  extends CommentBase1Crawler {
    @Autowired
    private  SinaCommentCrawler sinaCommentCrawler;
    @Autowired
    private  ITHomeCommentCrawler itHomeCommentCrawler;
    @Autowired
    private  QQCommentCrawler qqCommentCrawler;
    @Autowired
    private  WangyiCommentCrawler wangyiCommentCrawler;
    @Autowired
    private  MydriversCommentCrawler mydriversCommentCrawler;
    @Autowired
    private  IfengCommentCrawler ifengCommentCrawler;

    @Autowired
    private CommentMapper commentMapper;


    private Site site = Site.me().setRetryTimes(3).setSleepTime(500);
    //新浪
    private String sinaHrf="sina.cn";
    //it之家
    private String ithomeHrf="m.ithome.com";
    //腾讯
    private String xwqqHref="xw.qq.com";
    //网易
    private String wangyiHref="3g.163.com";
    //快科技
    private String mydriversHref="m.mydrivers.com";
    //凤凰网
    private String ifengHref="ifeng.com";

    private static SimpleDateFormat df = new SimpleDateFormat("yyyyMM");
    @Override
    synchronized public Spider createCrawler(List<NewsDO> newsList) {

        List<Request> requests = new ArrayList<>();
        for(NewsDO newsDO:newsList){
            String newsTime;
            if (null != newsDO.getAddTime() && null != newsDO.getNewsType()) {
                 newsTime = df.format(newsDO.getAddTime());
            }else{
                continue;
            }
            //查询评论表是否有50条评论 如果有则不抓了
            String cTable = TableUtil.getTableComment(newsDO.getNewsType(), newsTime);
            int commentCount;
            try {
                commentCount=commentMapper.getNewsCommentCount(cTable,newsDO.getNewsId());
            }catch (Exception e){
                e.printStackTrace();
                commentCount=0;
            }
            //如果大于50条评论则不进行抓取了
            if(commentCount>50){
                continue;
            }

            //解析处理腾讯的新闻获取cid
            if(newsDO.getNewsSourceUrl().contains(xwqqHref)){
                String url=newsDO.getNewsSourceUrl();
                Request request = new Request(url + "?newsId=" + newsDO.getNewsId());
                requests.add(request);
            }
            //解析处理新浪新闻页面的详情
            if(newsDO.getNewsSourceUrl().contains(sinaHrf)){
                //如果是t.cj开头的页面先不抓 规则不一样 评论几乎没有 不抓
                if(newsDO.getNewsSourceUrl().contains("t.cj.sina.cn")){
                    continue;
                }
                Request request = new Request(newsDO.getNewsSourceUrl() + "&newsId=" + newsDO.getNewsId());
                requests.add(request);
            }
            //解析处理it之家页面的详情
            if(newsDO.getNewsSourceUrl().contains(ithomeHrf)){
                String url=newsDO.getNewsSourceUrl();
                String itnewsId=url.substring(url.indexOf("html/")+5,url.lastIndexOf(".htm"));
                itHomeCommentCrawler.createCrawler(itnewsId,null,newsDO.getNewsId()).thread(1).run();
            }
            //解析处理网易页面的详情
            if(newsDO.getNewsSourceUrl().contains(wangyiHref)){
                String url=newsDO.getNewsSourceUrl();
                String wyiNewsId=url.substring(url.lastIndexOf("/")+1,url.indexOf(".html"));
                wangyiCommentCrawler.createCrawler(wyiNewsId,null,newsDO.getNewsId()).thread(1).run();
            }
//            //解析处理快科技
            if(newsDO.getNewsSourceUrl().contains(mydriversHref)){
                String url=newsDO.getNewsSourceUrl();
                String driverNewsId=url.substring(url.lastIndexOf("/")+1,url.indexOf(".html"));
                mydriversCommentCrawler.createCrawler(driverNewsId,null,newsDO.getNewsId()).thread(1).run();
            }
            //处理凤凰网评论
            if(newsDO.getNewsSourceUrl().contains(ifengHref)){
                Request request = new Request(newsDO.getNewsSourceUrl() + "?newsId=" + newsDO.getNewsId());
                requests.add(request);
            }

        }
        // create spider
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(requests.toArray(new Request[]{}));
        return spider;
    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();

        Html html = page.getHtml();
        Map<String, String> params = analyticalUrlParams(url);
        if (CollectionUtils.isEmpty(params)) {
            return;
        }
        String newsId = params.get("newsId");

        //如果是新浪的页面 ,__cmntTotal
        if(url.contains(sinaHrf)){

            String contentRegex = "newsid:(.*?),\n" +
                    "            product";
            String contentRegex1 = "product:(.*?),\n" +
                    "            index";
            String pushId=getSubUtilSimple(html.toString(),contentRegex);
            String product=getSubUtilSimple(html.toString(),contentRegex1);
            if(StringUtils.isEmpty(pushId) && StringUtils.isEmpty(product)){
                contentRegex = "newsid:(.*?),\n" +
                        "        encoding";
                contentRegex1 = "channel:(.*?),\n" +
                        "        newsid";
                pushId=getSubUtilSimple(html.toString(),contentRegex);
                product=getSubUtilSimple(html.toString(),contentRegex1);

            }

            //如果没有值则结束, product[^>]*
            if(StringUtils.isEmpty(pushId) && StringUtils.isEmpty(product)){
                List<Selectable> nodes=html.xpath("//meta[@name='publishid']/@content").nodes();
                if(nodes.size()>0){
                    pushId=nodes.get(0).toString();
                }
            }
            //如果pushId没有则删除
            if(StringUtils.isEmpty(pushId)){
                return;
            }
            pushId=pushId.replace("'","").trim();
            product=product.replace("'","").trim();
            sinaCommentCrawler.createCrawler(pushId,product,new Long(newsId)).thread(1).run();

        }
        //如果是腾讯的页面
        if(url.contains(xwqqHref)){
            String contentRegex = "cid :(.*?),\n" +
                    "        commentNumber";
            String cId=getSubUtilSimple(html.toString(),contentRegex);
            //如果pushId没有则删除
            if(StringUtils.isEmpty(cId)){
                return;
            }
            cId=cId.replace("\"","").trim();
            qqCommentCrawler.createCrawler(cId,null,new Long(newsId)).thread(1).run();
        }
        //如果是凤凰网的页面
        if(url.contains(ifengHref)){
            String contentRegex = "\"docUrl\":(.*?),\n" +
                    "                   \"summary\"";
            String cId=getSubUtilSimple(html.toString(),contentRegex);

            //如果pushId没有则删除
            if(StringUtils.isEmpty(cId)){
                String contentRegex1 = "\"docUrl\":(.*?),\n" +
                        "                            \"summary\"";
                cId=getSubUtilSimple(html.toString(),contentRegex1);
            }
            if(StringUtils.isEmpty(cId)){
                return;
            }
            cId=cId.replace("\"","").trim();
            ifengCommentCrawler.createCrawler(cId,null,new Long(newsId)).thread(1).run();
        }
    }
    @Override
    public Site getSite() {
        return site;
    }

    public static String getSubUtilSimple(String soap, String rgex) {
        Pattern pattern = Pattern.compile(rgex);// 匹配的模式
        Matcher m = pattern.matcher(soap);
        while (m.find()) {
            return m.group(1);
        }
        return "";
    }
}
