package cn.mc.scheduler.crawlerOverseas.economist.com;

import cn.mc.core.dataObject.NewsContentOverseasArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsContentOverseasArticleCoreManager;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.HtmlNodeUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.crawlerOverseas.OverseasCrawlerPipeline;
import cn.mc.scheduler.crawlerOverseas.readwrite.com.ReadWriteAiCrawler;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;
import us.codecraft.webmagic.utils.HttpConstant;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * www.economist.com 海外财经 - 最新栏目(最后更新)
 *
 * @author Sin
 * @time 2018/9/14 下午2:13
 */
@Component
public class EconomistNewestCrawler extends BaseCrawler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadWriteAiCrawler.class);
    private static final String URL = "https://www.economist.com/latest";
    private static final String DETAIL_URI = "https://www.economist.com/";
    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(300);

    private Map<String, NewsDO> cacheNewsMap = Maps.newHashMap();
    private Map<String, List<NewsImageDO>> cacheNewsImageMap = Maps.newHashMap();

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsContentOverseasArticleCoreManager overseasArticleCoreManager;
    @Autowired
    private OverseasCrawlerPipeline overseasCrawlerPipeline;

    @Override
    public Spider createCrawler() {
        Request request = new Request(URL);
        addHeader(request);
        request.setMethod(HttpConstant.Method.GET);
        return new BaseSpider(this).addRequest(request);
    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();
        if (url.equals(URL)) {
            handleList(page);
        } else {
            handleDetail(page);
        }
    }

    @Override
    public Site getSite() {
        return SITE;
    }

    ///
    /// 内容处理 z

    void handleList(Page page) {
        Html html = page.getHtml();
        List<Selectable> articleNodes = html.xpath("//div[@class='teaser-list']/article").nodes();

        if (CollectionUtils.isEmpty(articleNodes)) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("section 没找到！爬虫异常！");
            }
        }

        for (Selectable articleNode : articleNodes) {
            String imageUrl = articleNode.xpath("//div[@class='component-image']/img/@src").toString();
            String title = articleNode.xpath("//span[@class='flytitle-and-title__title']/allText()").toString();
            String newsAbstract = articleNode.xpath("//div[@class='teaser__text']/allText()").toString();
            String newsSource = NewsDO.DATA_SOURCE_ECONOMIST;
            String newsSourceUrl = DETAIL_URI + articleNode.xpath("//a/@href").toString();

            // 创建 newsId
            long newsId = IDUtil.getNewID();

            // 构建 newsImageDO
            long newsImageId = IDUtil.getNewID();
            int imageWidth = 0;
            int imageHeight = 0;
            NewsImageDO newsImageDO = newsImageCoreManager.buildNewsImageDO(
                    newsImageId, newsId,
                    imageUrl, imageWidth, imageHeight, NewsImageDO.IMAGE_TYPE_MINI);
            List<NewsImageDO> newsImageDOList = Lists.newArrayList(newsImageDO);

            // 构建 newsDO
            String dataKey = encrypt(newsSourceUrl);
            int newsHot = 0;
            String newsUrl = "";
            String shareUrl = "";
            int newsType = NewsDO.NEWS_TYPE_FINANCE_OVERSEAS;
            int contentType = NewsDO.CONTENT_TYPE_IMAGE_TEXT;
            int displayType = NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE;
            String keywords = "";
            int banComment = 0;
            Date displayTime = DateUtil.currentDate();
            int videoCount = 0;
            int imageCount = newsImageDOList.size();
            Date createTime = DateUtil.currentDate();
            int sourceCommentCount = 0;
            int commentCount = 0;

            NewsDO newsDO = newsCoreManager.buildNewsDO(newsId, dataKey, title,
                    newsHot, newsUrl, shareUrl, newsSource, newsSourceUrl,
                    newsType, contentType, keywords, banComment, newsAbstract,
                    displayTime, videoCount, imageCount, createTime,
                    displayType, sourceCommentCount, commentCount);

            // 添加到 cache
            cacheNewsMap.put(dataKey, newsDO);
            cacheNewsImageMap.put(dataKey, newsImageDOList);

            // 添加 detail 页面地址
            page.addTargetRequest(newsSourceUrl);
        }
    }

    void handleDetail(Page page) {
        String url = page.getUrl().toString();
        String dataKey = encrypt(url);
        Html html = page.getHtml();
        NewsDO newsDO = cacheNewsMap.get(dataKey);
        List<NewsImageDO> newsImageDOList = cacheNewsImageMap.get(dataKey);

        if (newsDO == null) {
            return;
        }

        // 获取文章内容 node
        Selectable contentNode = html.xpath("//article/div[@class='blog-post__inner']");

        // 需要清楚的 nodes
        List<String> clearXpathList = ImmutableList.of(
                "//div[@class='blog-post__asideable-wrapper']"
        );

        for (String xpath : clearXpathList) {
            List<Selectable> nodes = contentNode.xpath(xpath).nodes();
            HtmlNodeUtil.removeLabel(nodes);
        }

        // 获取文章内容 node
        String articleHtml = contentNode.toString();

        // 构建海外内容 article
        long newsId = newsDO.getNewsId();
        String title = newsDO.getTitle();
        long newsContentOverseasArticleId = IDUtil.getNewID();
        NewsContentOverseasArticleDO overseasArticleDO
                = overseasArticleCoreManager.buildOverseasArticleDO(
                newsContentOverseasArticleId,
                newsId,
                articleHtml,
                title
        );

        // 去 save 数据
        overseasCrawlerPipeline.save(newsDO, newsImageDOList, overseasArticleDO);
    }


    ///
    /// tools

    void addHeader(Request request) {

//        :authority: www.economist.com
//:method: GET
//        :path: /latest/
//:scheme: https
//        accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8
//accept-encoding: gzip, deflate, br
//accept-language: zh-CN,zh;q=0.9,en;q=0.8
//cache-control: no-cache
//cookie: geo_country=CN; geo_region=AP; rvjourney=RevampMVT2/50/50/RevampMVT2; rvuuid=0a57bcad67e716df15bc694d9b9a1b9a; abversion=d; visid_incap_121505=4n18w6cZR82LFZAVc8ybZcVQm1sAAAAAQUIPAAAAAACAOpMbIvgms7E5ub8E9jAL; incap_ses_893_121505=R0PbOKTGVg+09nuOO5NkDMVQm1sAAAAAUoLVUlgyR2SzLtxnofYhlQ==; s_cc=true; s_fid=187BE0F209ADDDA6-2890A8FAFAE43033; s_sq=%5B%5BB%5D%5D; _ga=GA1.2.815177016.1536905418; _gid=GA1.2.994830721.1536905418; _cb_ls=1; _cb=Hu7ImDTSGOsBz8nio; _cb_svref=null; s_vi=[CS]v1|2DCDA86505078763-4000010B0001FC17[CE]; seerses=e; seerses=e; seerid=102026.90833339324; seerid=102026.90833339324; _sp_ses.6168=*; __gads=ID=c052096ccf4a3108:T=1536905418:S=ALNI_MZCpbVBpeFdNAE-kRESZlK_nhauTQ; _litra_ses.9b72=*; __adblocker=false; __pnahc=0; __tbc=%7Bjbd%7DeyJ2IjozLCJwayI6Iks0NktvU2JmdjVoQnEwYVVMME5qVFhkbW5TUVhOQ2FNdWFJZXdjN2cxbXRsNEN0WGUyajhuRXZReFpvciIsInNrIjoiQ2dlblhycHJnSCJ9; __pat=-14400000; xbc=%7Bjbd%7DeyJ2IjozLCJwayI6Iks0NktvU2JmdjVoQnEwYVVMME5qVFhkbW5TUVhOQ2FNdWFJZXdjN2cxbXRsNEN0WGUyajhuRXZReFpvciIsInNrIjoiQ2dlblhycHJnSCJ9; s_prop50=other%20ref; s_prop65=o%2Foth%2Fnone%2Fnone%2Fnone%2Fnone%2Fearned%2F201891414%3A10%2Fother%20ref%2Fnone%2Fnone%2Fnone%2Fnone%2Fnone%2Fnone; s_evar23=o%2Foth%2Fnone%2Fnone%2Fnone%2Fnone%2Fearned%2F201891414%3A10%2Fother%20ref%2Fnone%2Fnone%2Fnone%2Fnone%2Fnone%2Fnone; s_evar50=other%20ref; ec_device=false; btIdentify=3c37a476-067c-4468-9797-d27d95ff4659; _bts=8ce805f5-7046-488f-cd7a-24d44d1a9a7c; _evidon_consent_cookie={"consent_date":"2018-09-14T06:10:39.126Z"}; incap_ses_576_121505=enLVRg53YmMsp7bz1l3+B8FRm1sAAAAASwSzCZn7/VpSTioqa7ybhA==; GED_PLAYLIST_ACTIVITY=W3sidSI6ImY0Sk0iLCJ0c2wiOjE1MzY5MDYyMzQsIm52IjoxLCJ1cHQiOjE1MzY5MDYyMzIsImx0IjoxNTM2OTA2MjMyfV0.; lytics_bluekai_ts=1536906305232; nlbi_121505=R/h5RhFDhBVvYlJ7NCSsiwAAAAC/hR5LGSWpxTf2+LH6rbGJ; _gat_UA-77249134-9=1; ly_segs=%7B%22suppression_current_customers%22%3A%22suppression_current_customers%22%2C%22trump_biz%22%3A%22trump_biz%22%2C%22trump%22%3A%22trump%22%2C%22biz%22%3A%22biz%22%2C%22brexit%22%3A%22brexit%22%2C%22freq%22%3A%22freq%22%2C%22trump_biz_non_sub%22%3A%22trump_biz_non_sub%22%2C%22open_future_master%22%3A%22open_future_master%22%2C%22unknowns%22%3A%22unknowns%22%2C%22excl_mas_sup%22%3A%22excl_mas_sup%22%2C%22excl_mas_sup1%22%3A%22excl_mas_sup1%22%2C%22smt_new%22%3A%22smt_new%22%2C%22aspect_score_frequent%22%3A%22aspect_score_frequent%22%2C%22aspect_score_lowusage%22%3A%22aspect_score_lowusage%22%2C%22aspect_score_notreengage%22%3A%22aspect_score_notreengage%22%2C%22aspect_email_unknown%22%3A%22aspect_email_unknown%22%2C%22all%22%3A%22all%22%7D; _gat=1; __pvi=%7B%22id%22%3A%22v-2018-09-14-14-10-20-070-yqlOW0NNEx3fFFo6-620fd45a24dfbcce41b59e05084c5206%22%2C%22domain%22%3A%22.economist.com%22%2C%22time%22%3A1536906843424%7D; _chartbeat2=.1536905418485.1536906843459.1.Be__b_DTLJjahcACBDEOvlKD0CUx3.19; _pk_id.TV-81903654-1.9b72=6e91710a67ba88cf.1536905419.0.1536906843..; _sp_id.6168=97fcf394-1d77-4220-a60f-558daf32bf9e.1536905419.1.1536906844.1536905419.59c95b41-81ed-4e9f-bbd0-37fe52a76374; _litra_id.9b72=a-00qj--e63b296d-dbbf-4594-8aa9-bc9027d44fbc.1536905420.1.1536906844.1536905420.3a360f6f-21e3-451b-9cae-5b7a3d496c50; s_econcpm2=%5B%5B%27other%20ref%27%2C%271536906843743%27%5D%5D; _bti=%7B%22app_id%22%3A%22economist-prod%22%2C%22bsin%22%3A%224XJCLyac%2FuujeoZfnzfGF53orIhJT3pxCZ4F7kOXTS%2FpGrLsgiWIreNOzUzmXj6Tq3y0HreXq9A4DoXeuCX%2BjA%3D%3D%22%2C%22user_id%22%3A%22e3c0b3c5-484b-4933-96b3-889f85d5204e%3A1536905419.57%22%7D; PathforaPageView=12; mmapi.store.p.0=%7B%22mmparams.d%22%3A%7B%7D%2C%22mmparams.p%22%3A%7B%22uat%22%3A%221568442842559%7C%7B%5C%22LogInState%5C%22%3A%5C%22LoggedOut%5C%22%7D%22%2C%22pd%22%3A%221568442844882%7C%5C%22310974946%7CQwAAAApVAwBM4iVZshCcVgABEQABQsiaaGsBAOjuEREMGtZImRilvQga1kgAAAAA%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F8ABkRpcmVjdAGyEAEAAAAAAAAAAACwbgAAypQAAJbbAAACAHalAAAi9NcDOrIQADCfAAAFshCyEP%2F%2FDQAAAQAAAAAB3bcBAGc5AgAAuOkAACBs41YRshAA%2F%2F%2F%2F%2FwGyELIQ%2F%2F8SAAABAAAAAAGvUAIAJgADAAAAAAAAAAFF%5C%22%22%2C%22srv%22%3A%221568442844885%7C%5C%22lvsvwcgeu01%5C%22%22%7D%2C%22T75_HardWall_Test%22%3A%7B%7D%2C%22mmengine%22%3A%7B%22mm_return_visit%22%3A%221552458844587%7C12%22%7D%7D; mmapi.store.s.0=%7B%22mmparams.d%22%3A%7B%7D%2C%22mmparams.p%22%3A%7B%7D%2C%22T75_HardWall_Test%22%3A%7B%22adobe-integration%22%3A%220%7C%5C%22element1%3Aworld%5C%22%22%7D%2C%22mmengine%22%3A%7B%7D%7D; utag_main=v_id:0165d6b3932f00200f523159765203079001a07100ac2$_sn:1$_ss:0$_pn:12%3Bexp-session$_st:1536908658321$ses_id:1536905417519%3Bexp-session
//pragma: no-cache
//upgrade-insecure-requests: 1
//user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36

        request.addHeader(":authority", "www.economist.com");
        request.addHeader(":method", "GET");
        request.addHeader(":path", "/latest/");
        request.addHeader(":scheme", "http");
        request.addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        request.addHeader("upgrade-insecure-requests", "1");
        request.addHeader("user-agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1");
    }
}
