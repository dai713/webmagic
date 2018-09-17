package cn.mc.scheduler.crawler.weixin.sogou.com;

import cn.mc.core.dataObject.NewsContentArticleDO;
import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.manager.NewsContentArticleCoreManager;
import cn.mc.core.manager.NewsCoreManager;
import cn.mc.core.manager.NewsImageCoreManager;
import cn.mc.core.utils.DateUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.base.BaseSpider;
import cn.mc.scheduler.crawler.BaseCrawler;
import cn.mc.scheduler.util.HtmlNodeUtil;
import cn.mc.scheduler.util.SchedulerUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Selectable;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 微信-搜狗新闻
 *
 * @author daiqingwen
 * @date 2018/7/23 下午 17:46
 */
@Component
public class WeiXinSoGouCrawler extends BaseCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeiXinSoGouCrawler.class);

    private Site SITE = Site.me().setRetrySleepTime(3).setSleepTime(300);

    private static final String url = "http://weixin.sogou.com/";

    @Autowired
    private NewsCoreManager newsCoreManager;
    @Autowired
    private NewsImageCoreManager newsImageCoreManager;
    @Autowired
    private NewsContentArticleCoreManager newsContentArticleCoreManager;
    @Autowired
    private SchedulerUtils schedulerUtils;
    @Autowired
    private WeiXinSoGouPipeline weiXinSoGouPipeline;

    private Map<String, NewsDO> cacheNews = Maps.newHashMap();
    private Map<String, List<NewsImageDO>> cacheNewsImages = Maps.newHashMap();
    private Map<String, Object> cachePosition = Maps.newHashMap();


    @Override
    public Spider createCrawler() {
        Request request = new Request();
        request.setUrl(url);
        BaseSpider spider = new BaseSpider(this);
        spider.addRequest(request);
        return spider;
    }

    @Override
    public void process(Page page) {
        if (url.equals(page.getUrl().toString())) {
            getList(page);
        } else {
            getDetails(page);
        }
    }

    @Override
    public Site getSite() {
        return SITE;
    }

    /**
     * 获取新闻列表
     *
     * @param page
     */
    private void getList(Page page) {
        try {
            Html html = page.getHtml();
            List<Selectable> nodes = html.xpath("//ul[@id='pc_0_0']/li").nodes();
            // 获取第一条数据
            Selectable select = nodes.get(0).xpath("//div[@class='txt-box']");
            String firstTitle = select.xpath("//h3/a/text()").toString();

            if (cachePosition.containsKey(firstTitle)) {
                return;
            } else {
                if (!CollectionUtils.isEmpty(cachePosition)) {
                    cachePosition.clear();
                }
                cachePosition.put(firstTitle, null);
            }

            for (int i = 0; i < nodes.size(); i++) {
                Selectable selectable = nodes.get(i);
                // 文字元素
                Selectable item = selectable.xpath("//div[@class='txt-box']");
                // 图片元素
                Selectable imgItem = selectable.xpath("//div[@class='img-box']");

                // 数据
                String title = item.xpath("//h3/a/text()").toString();
                String newsAbstract = select.xpath("//p[@class='txt-info']/text()").toString();
                if (newsAbstract.length() > 250) {
                    newsAbstract = newsAbstract.substring(0, 250);
                }

                String href = item.xpath("//a/@href").toString();
                String source = item.xpath("//a[@class='account']/text()").toString();
                String imageUrl = "http:" + imgItem.xpath("//a/img/@src").toString();
                String dataKey = encrypt(href);

                // 没图片就不要了
                if (StringUtils.isEmpty(imageUrl)) {
                    return;
                }

                Integer imageWidth;
                Integer imageHeight;
                Map<String, Integer> map = schedulerUtils.parseImg(imageUrl);
                if (StringUtils.isEmpty(map)) {
                    imageWidth = 0;
                    imageHeight = 0;
                } else {
                    imageWidth = map.get("width");
                    imageHeight = map.get("height");
                }

                long newsId = IDUtil.getNewID();

                // 处理图片
                List<NewsImageDO> newsImageDOList = Lists.newArrayList();
                NewsImageDO newsImageDO = newsImageCoreManager.buildNewsImageDO(
                        IDUtil.getNewID(), newsId, imageUrl,
                        imageWidth, imageHeight, NewsImageDO.IMAGE_TYPE_MINI);

                newsImageDOList.add(newsImageDO);
                cacheNewsImages.put(dataKey, newsImageDOList);

                // 处理 news
                int newsHot = 0;
                String newsUrl = "";
                String shareUrl = "";
                int newsType = NewsDO.NEWS_TYPE_HEADLINE;
                int contentType = NewsDO.CONTENT_TYPE_IMAGE_TEXT;
                int banComment = 0;
                Date displayTime = cn.mc.core.utils.DateUtil.currentDate();
                int videoCount = 0;
                int imageCount = newsImageDOList.size();
                int displayType = handleNewsDisplayType(imageCount);
                int commentCount = 0;
                int sourceCommentCount = 0;
                String newsSource = source;
                String newsSourceUrl = href;
                String keywords = "";
                Date createTime = DateUtil.currentDate();

                NewsDO newsDO = newsCoreManager.buildNewsDO(newsId, dataKey, title, newsHot,
                        newsUrl, shareUrl, newsSource, newsSourceUrl, newsType,
                        contentType, keywords, banComment, newsAbstract, displayTime,
                        videoCount, imageCount, createTime, displayType,
                        sourceCommentCount, commentCount);

                // 添加到缓存
                cacheNews.put(dataKey, newsDO);

                // 添加 detail 页面 request
                page.addTargetRequest(newsSourceUrl);
            }
        } catch (Exception e) {
            LOGGER.error("抓取搜狗微信新闻失败：{}", ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * 获取新闻详情
     *
     * @param page
     */
    public void getDetails(Page page) {
        String url = page.getUrl().toString();
        String dataKey = encrypt(url);

        if (!cacheNews.containsKey(dataKey)
                || !cacheNewsImages.containsKey(dataKey)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("dataKey 不存在，自动过滤!");
            }
            return;
        }

        Selectable contentNode = page.getHtml().xpath("//div[@id='js_content']");
        String content = contentNode.toString();
        if (StringUtils.isEmpty(content)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("新闻 content 为空! 自动过滤!");
            }
            return;
        }

        ///
        /// 过滤内容格式，严格按照顺序执行。

        // 处理图片 cdn、广告、懒加载图片
        List<Selectable> imageNodes = contentNode.xpath("//img").nodes();
        if (!CollectionUtils.isEmpty(imageNodes)) {

            // 过滤广告，如果是广告则删除广告
            filterImageAdvertising(imageNodes);

            // 处理 dataSrc
            String sourceAttr = "data-src";
            String targetAttr = "src";
            HtmlNodeUtil.handleCdnWithRemoveSourceAttr(imageNodes, sourceAttr, targetAttr);
        }

        // 删除 iFrame 节点，存在视频或者其他广告
        List<Selectable> iFrameNodes = contentNode.xpath("//iframe").nodes();
        if (!CollectionUtils.isEmpty(iFrameNodes)) {
            for (Selectable iFrameNode : iFrameNodes) {
                List<Element> elementList
                        = HtmlNodeUtil.getElements((HtmlNode) iFrameNode);
                removeNode(elementList.get(0));
            }
        }

        // 去掉一些没用的标题
        List<Selectable> sectionNodes = contentNode.xpath("//section").nodes();
        if (!CollectionUtils.isEmpty(sectionNodes)) {
            // 判断最后一个 section，如果中间存在 a 链接，则删除这个 section 节点

            int i = 0;
            for (Selectable sectionNode : sectionNodes) {
                if (sectionNode.xpath("//a").nodes().size() > 0) {
                    List<Element> elementList
                            = HtmlNodeUtil.getElements((HtmlNode) sectionNode);
                    removeNode(elementList.get(0));

                    if (sectionNodes.size() <= 1) {
                        continue;
                    }

                    // 处理空标题 (有标题，然后下面没内容了)
                    Selectable preSectionNode = sectionNodes.get(i - 1);
                    int preSectionPNodeSize = preSectionNode.xpath("//strong").nodes().size();
                    int preSectionStrongNodeSize = preSectionNode.xpath("//strong").nodes().size();
                    if (preSectionPNodeSize < 1 && preSectionStrongNodeSize > 1) {
                        List<Element> preElementList
                                = HtmlNodeUtil.getElements((HtmlNode) preSectionNode);
                        removeNode(preElementList.get(0));
                    }
                }

                i++;
            }
        }

        // 去除所有 <a> 标签
        List<Selectable> aLinkNodes = contentNode.xpath("//a").nodes();
        if (!CollectionUtils.isEmpty(aLinkNodes)) {
            // 判断最后一个 section，如果中间存在 a 链接，则删除这个 section 节点
            for (Selectable sectionNode : aLinkNodes) {
                List<Element> elementList
                        = HtmlNodeUtil.getElements((HtmlNode) sectionNode);
                removeNode(elementList.get(0));
            }
        }

        // 处理末尾内容
        List<Selectable> pNodes = contentNode.xpath("//p").nodes();
        if (!CollectionUtils.isEmpty(pNodes)) {

            Set<Selectable> filterNodes = Sets.newHashSet();
            if (pNodes.size() > 3) {
                filterNodes.addAll(pNodes.subList(pNodes.size() - 10, pNodes.size()));
            }

            for (Selectable filterNode : filterNodes) {
                int contentLength = filterNode.xpath("//tidyText()")
                        .replace("\\n|\\r", "").toString().length();
                if (contentLength < 10) {
                    List<Element> elementList
                            = HtmlNodeUtil.getElements((HtmlNode) filterNode);
                    removeNode(elementList.get(0));
                }
            }
        }

        // 重新获取 content
        content = contentNode.toString();

        // 处理新闻标签
        content = schedulerUtils.replaceLabel(content);
        content = otherLabel(content);

        // 获取 news 信息
        NewsDO newsDO = cacheNews.get(dataKey);
        long newsId = newsDO.getNewsId();

        // 构建一个文章
        NewsContentArticleDO newsContentArticleDO
                = newsContentArticleCoreManager.buildNewsContentArticleDO(
                        IDUtil.getNewID(), newsId, content);

        // 获取图片
        List<NewsImageDO> newsImageDOList = cacheNewsImages.get(dataKey);

        // 去保存
        weiXinSoGouPipeline.save(newsDO, newsContentArticleDO, newsImageDOList);
    }

    /**
     * 过滤广告动图
     *
     * @param imageNodes
     */
    private void filterImageAdvertising(List<Selectable> imageNodes) {
        // 处理前 3 后 3 图片，不满足处理全部
        Set<Selectable> filterNodes = Sets.newHashSet();
        if (imageNodes.size() > 3) {
            filterNodes.addAll(imageNodes.subList(0, 3));
            filterNodes.addAll(imageNodes.subList(imageNodes.size() - 4, imageNodes.size()));
        } else {
            filterNodes.addAll(imageNodes);
        }

        for (Selectable imageNode : filterNodes) {
            filterImageAdvertising(imageNode);
        }
    }

    /**
     * 过滤广告 - 图片
     *
     * @param imageNode
     */
    private void filterImageAdvertising(Selectable imageNode) {
        // 如果是广告必然有 1 element
        List<Element> elementList = HtmlNodeUtil.getElements((HtmlNode) imageNode);
        Element element = elementList.get(0);
        String imageUrl = element.attr("src");
        boolean hasAdvertising = imageUrl.matches(".*(gif).*");
        if (!hasAdvertising) {
            return;
        }
        removeNode(element);
    }

    /**
     * 替换所有其他链接标签
     *
     * @param content
     * @return String
     */
    private String otherLabel(String content) {
        String reg = "<section[^>]*?>[\\s\\S]*?</section[^>]*?>";
        Pattern p = Pattern.compile(reg);
        Matcher m = p.matcher(content);
        while (m.find()) {
            content = m.replaceAll("");
        }
        return content;
    }

    /**
     * 删除节点
     *
     *  <p>
     *     element parent 节点为 null，删除检查
     *     Exception (Object must not be null)
     *  </p>
     *
     * @param element
     */
    private void removeNode(Element element) {
        // TODO: 2018/7/26 Object must not be null 会导致删除 element 节点失败
        try {
            element.remove();
        } catch (Exception e) {
            // skip
        }
    }
}
