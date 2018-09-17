package cn.mc.scheduler.crawlerPaper;

import cn.mc.core.client.FileClient;
import cn.mc.core.client.FileObject;
import cn.mc.core.constants.CodeConstants;
import cn.mc.core.constants.FileConstants;
import cn.mc.core.dataObject.NewsImageDO;
import cn.mc.core.dataObject.paper.PaperDO;
import cn.mc.core.dataObject.paper.PaperNewsDO;
import cn.mc.core.dataObject.paper.PaperNewsImageDO;
import cn.mc.core.entity.Page;
import cn.mc.core.mybatis.Field;
import cn.mc.core.utils.*;
import cn.mc.scheduler.mapper.PaperMapper;
import cn.mc.scheduler.mapper.PaperNewsMapper;
import cn.mc.scheduler.util.HtmlNodeUtil;
import cn.mc.scheduler.util.SchedulerUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Selectable;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 报纸爬虫
 *
 * @author Sin
 * @time 2018/9/4 下午5:49
 */
@Component
public class PaperCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaperCrawler.class);
    private static final int PAGE_SIZE = 100;

    @Autowired
    private PaperMapper paperMapper;
    @Autowired
    private PaperNewsMapper paperNewsMapper;
    @Autowired
    private PaperPipeline paperPipeline;

    public void exec() {
        int status = PaperDO.STATUS_NORMAL;
        int count = paperMapper.getPaperCount(status);
        if (count <= 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("paper count 小于 0 ! count {}", count);
            }
            return;
        }

        // 分页抓取
        List<Page> pages = CommonUtil.buildPages(count, PAGE_SIZE);
        for (Page page : pages) {

            // 获取抓取的 paper
            List<PaperDO> paperDOList = paperMapper
                    .listPaperPage(page, status, new Field());

            if (CollectionUtils.isEmpty(paperDOList)) {
                return;
            }

            // 开始抓取
            start(paperDOList);
        }
    }

    public void start(List<PaperDO> paperDOList) {
        for (PaperDO paperDO : paperDOList) {
            Long paperId = paperDO.getId();

            // 获取 page list
            List<PaperCrawlerPageList> pageLists = getPageList(paperDO);

            // 如果 pageListUrl 没有，则抓取失败
            if (CollectionUtils.isEmpty(pageLists)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("没有获取到 page urls! 抓取失败!");
                }
                return;
            }

            // 获取每个 pageList 的 newsList
            List<PaperCrawlerNews> newsList = getNewsList(paperDO, pageLists);
            if (CollectionUtils.isEmpty(newsList)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("没有获取到 newsList! 抓取失败!");
                }
                return;
            }

            // 开始获取 news 内容
            List<PaperCrawlerNews> newsContentList = getNewsContent(paperDO, newsList);

            int index = 0;
            for (PaperCrawlerNews paperCrawlerNews : newsContentList) {
                PaperCrawlerPageList pageList = paperCrawlerNews.getPaperCrawlerPageList();

                // paperNewsId
                Long paperNewsId = IDUtil.getNewID();

                // 构建图片

                List<PaperNewsImageDO> newsImages = Lists.newArrayList();
                List<String> images = paperCrawlerNews.getImages();

                if (!CollectionUtils.isEmpty(images)) {
                    for (String image : images) {
                        PaperNewsImageDO paperNewsImageDO = new PaperNewsImageDO();
                        paperNewsImageDO.setId(IDUtil.getNewID());
                        paperNewsImageDO.setPaperNewsId(paperNewsId);
                        paperNewsImageDO.setImageHeight(0);
                        paperNewsImageDO.setImageWidth(0);
                        paperNewsImageDO.setImageType(NewsImageDO.IMAGE_TYPE_MINI);
                        paperNewsImageDO.setImageUrl(image);
                        paperNewsImageDO.setStatus(NewsImageDO.STATUS_NORMAL);
                        newsImages.add(paperNewsImageDO);
                    }
                }

                // 开始构建 paperNews
                PaperNewsDO paperNewsDO = new PaperNewsDO();

                paperNewsDO.setId(paperNewsId);
                paperNewsDO.setPaperId(paperId);
                paperNewsDO.setPaperNumber(paperDO.getPaperNumber());

                paperNewsDO.setTitle(paperCrawlerNews.getTitle());
                paperNewsDO.setNewsAbstract(paperCrawlerNews.getNewsAbstract());
                paperNewsDO.setContent(paperCrawlerNews.getContent());

                paperNewsDO.setSourceUrl(paperCrawlerNews.getNewsUrl());
                paperNewsDO.setPageNumber(pageList.getPageNumber());

                paperNewsDO.setDataKey(paperCrawlerNews.getDataKey());
                paperNewsDO.setSort(index);
                paperNewsDO.setStatus(PaperNewsDO.STATUS_NORMAL);
                paperNewsDO.setCreateTime(DateUtil.currentDate());
                paperNewsDO.setImageCount(newsImages.size());

                // 用于 sort 排序
                index++;

                // 去保存
                paperPipeline.save(paperNewsDO, newsImages);
            }
        }
    }

    private List<PaperCrawlerNews> getNewsContent(
            PaperDO paperDO, List<PaperCrawlerNews> newsList) {

        List<PaperCrawlerNews> result = Lists.newArrayList();
        for (PaperCrawlerNews news : newsList) {
            // 获取 paper 新闻的 内容
            String newsUrl = news.getNewsUrl();

            // 抓取 html
            String resultHtml = requestHtml(newsUrl);
            if (StringUtils.isEmpty(resultHtml)) {
                continue;
            }

            // 创建 htmlNode
            HtmlNode htmlNode = Html.create(resultHtml);

            // 获取 content 内容
            String[] contentXpathArray = paperDO.getContentXpath().split(",");
            StringBuffer htmlBuffer = new StringBuffer();
            for (String contentXpath : contentXpathArray) {
                Selectable contentNode = htmlNode.xpath(contentXpath);

                if (StringUtils.isEmpty(contentNode.nodes())) {
                    continue;
                }
                htmlBuffer.append(contentNode.toString());
            }

            if (StringUtils.isEmpty(htmlBuffer.toString())) {
                // 么有 content 内容
                continue;
            }

            // 重新创建 html 节点
            HtmlNode contentNode = Html.create(htmlBuffer.toString());

            // 二次检查空内容
            String allText = SchedulerUtils.filterText(contentNode.xpath("allText()").toString());
            int allTextLength = allText.length();
            if (allTextLength <= 3) {
                continue;
            }

            // 提取图片 (优先处理图片)
            List<String> images = Lists.newArrayList();
            List<Selectable> imageNodes = contentNode.xpath("//img").nodes();

            for (Selectable imageNode : imageNodes) {

                // 获取 element
                List<Element> imageElements = HtmlNodeUtil.getElements((HtmlNode) imageNode);
                Element imageElement = imageElements.get(0);

                // 处理图片 url
                // ../../../images/2018-09/07/01/2953718_donghf_1536252733498_b.jpg
                String imageUrl = imageElement.attr("src");
                String newImageUrl;
                if (imageUrl.startsWith("/")) {
                    try {
                        URL url = new URL(newsUrl);
                        String host = url.getHost();
                        String httpUrl = "http://" + host;
                        newImageUrl = httpUrl + imageUrl;
                    } catch (Exception e) {
                        // skip
                        newImageUrl = null;
                    }
                } else {
                    String imageRegex = "\\.\\.\\/";
                    int count = CommonUtil.matchesCount(imageRegex, imageUrl);

                    StringBuffer imageUrlBuffer = new StringBuffer();
                    String[] urlArrays = newsUrl.split("/");
                    int appendMaxSize = urlArrays.length - count - 1;
                    for (int i = 0; i < urlArrays.length; i++) {
                        if (i < appendMaxSize) {
                            imageUrlBuffer.append(urlArrays[i]);
                            imageUrlBuffer.append("/");
                        }
                    }

                    String imageUri = imageUrl.replaceAll(imageRegex, "");
                    imageUrlBuffer.append(imageUri);
                    newImageUrl = imageUrlBuffer.toString();
                }

                if (StringUtils.isEmpty(newImageUrl)) {
                    continue;
                }

                // 上传 oss
                FileObject fileObject = FileClient
                        .uploadWithUrl(newImageUrl, FileConstants.PAPER_IMAGE);

                if (fileObject == null) {
                    continue;
                }

                String fileUrl = fileObject.getFileUrl();
                // 添加到 result
                images.add(fileUrl);

                // 更新 img 的 src
                imageElement.attr("src", fileUrl);
            }

            // 获取 contentNode 的 html(在 提取图片后获取 html，得到的是提取后的内容)
            String content = contentNode.toString();
            content = SchedulerUtils.filterHtmlLabel(content);

            // 检查 title 是否以获取到，没有则二次获取
            String titleXpath = paperDO.getNewsTitleXpath();
            Selectable titleNode = htmlNode.xpath(titleXpath);
            String title= titleNode.xpath("//allText()").toString();

            // 如果两次处理 title 还是不存在，就不要了
            if (StringUtils.isEmpty(news.getTitle()) && StringUtils.isEmpty(title)) {
                continue;
            }

            String filterText = SchedulerUtils.filterText(news.getTitle());
            if (StringUtils.isEmpty(filterText)) {
                news.setTitle(title);
            }

            // 添加到 images
            news.setImages(images);

            // 获取 md5 值
            String dataKey = EncryptUtil.encrypt(newsUrl, "md5");

            // 设置抓取的值
            news.setContent(content);
            news.setDataKey(dataKey);

            // 获取 摘要信息
            String contentText = SchedulerUtils.filterText(
                    contentNode.xpath("//allText()").toString());

            if (contentText.length() < 50) {
                continue;
            }
            String newsAbstract;
            if (contentText.length() > 60) {
                newsAbstract = contentText.substring(0, 60);
            } else {
                newsAbstract = contentText;
            }

            // 设置 摘要
            news.setNewsAbstract(newsAbstract);

            // 添加到 result
            result.add(news);
        }

        return result;
    }


    private List<PaperCrawlerNews> getNewsList(PaperDO paperDO, List<PaperCrawlerPageList> pageLists) {

        List<PaperCrawlerNews> newsList = Lists.newArrayList();
        for (PaperCrawlerPageList pageList : pageLists) {

            // 抓取内容
            String resultHtml = requestHtml(pageList.getPageListUrl());

            // 没有抓到内容
            if (StringUtils.isEmpty(resultHtml)) {
                continue;
            }

            // 内容的 htmlNode
            Html htmlNode = Html.create(resultHtml);

            // 转换 html node，用于 xpath 查找
            String newsListXpath = paperDO.getNewsListXpath();
            List<Selectable> newsListNodes = htmlNode.xpath(newsListXpath).nodes();

            for (Selectable newsListNode : newsListNodes) {
                String newsUrl = newsListNode.xpath("//a/@href").toString();
                String title = newsListNode.xpath("allText()").toString();

                // build url 地址
                String buildUrl = buildUrl(newsUrl, pageList.getPageListUrl());

                PaperCrawlerNews paperCrawlerNews = new PaperCrawlerNews();
                paperCrawlerNews.setTitle(title);
                paperCrawlerNews.setNewsUrl(buildUrl);
                paperCrawlerNews.setPaperCrawlerPageList(pageList);
                newsList.add(paperCrawlerNews);
            }
        }
        return newsList;
    }

    private List<PaperCrawlerPageList> getPageList(PaperDO paperDO) {

        // 获取 matcher 表达式（此处为 xpath)
        String pageListXpath = paperDO.getPageListXpath();

        // 获取请求地址
        String pageUrl = paperDO.getPageUrl();

        // 处理 pageUrl 中的动态参数
        pageUrl = PaperUrlFactory.factory(pageUrl);
        if (StringUtils.isEmpty(pageUrl)) {
            return Collections.EMPTY_LIST;
        }

        // 获取 html
        String requestHtml = requestHtml(pageUrl);
        if (StringUtils.isEmpty(requestHtml)) {
            return Collections.EMPTY_LIST;
        }

        // 转换 html node，用于 xpath 查找
        Html htmlNode = Html.create(requestHtml);

        // xpath 获取节点
        List<Selectable> pageNodes = htmlNode.xpath(pageListXpath).nodes();
        if (CollectionUtils.isEmpty(pageNodes)) {
            return Collections.EMPTY_LIST;
        }

        // 获取 url
        List<PaperCrawlerPageList> result = Lists.newArrayList();
        for (Selectable pageNode : pageNodes) {

            // 获取 url
            String pageNodeUrl = pageNode.xpath("//a/@href").toString();
            if (StringUtils.isEmpty(pageNodeUrl)) {
                continue;
            }

            // 处理 url
            String newPageListUrl = buildUrl(pageNodeUrl, paperDO.getPageUrl());

            // 获取 pageListText
            String pageListText = pageNode.xpath("//allText()").toString();
            if (StringUtils.isEmpty(pageListText)) {
                continue;
            }

            // array 必须大于= 2 （第02版：要闻）
            String[] splitArray = new String[]{};
            List<String> splits = ImmutableList.of("：", ":", ",", ".");
            for (String split : splits) {
                splitArray = pageListText.split(split);
                if (splitArray.length >= 2) {
                    break;
                }
            }

            // 如果分割符，不存在分布内容不存在
            if (splitArray.length <= 0) {
                splitArray = new String[]{pageListText};
            }

            String pageNumber;
            String pageListTitle;
            if (splitArray.length >= 2) {
                pageNumber = SchedulerUtils.filterText(splitArray[0]);
                pageListTitle = SchedulerUtils.filterText(splitArray[1]);
            } else {
                pageNumber = "";
                pageListTitle = SchedulerUtils.filterText(splitArray[0]);
            }

            PaperCrawlerPageList pageList = new PaperCrawlerPageList();
            pageList.setPageListText(pageListText);
            pageList.setPageNumber(pageNumber);
            pageList.setPageListTitle(pageListTitle);
            pageList.setPageListUrl(newPageListUrl);
            result.add(pageList);
        }
        return result;
    }

    private String requestHtml(String url) {

        String defaultCharset = "utf-8";
        // 发送请求
        String resultHtml = request(url, defaultCharset);

        if (StringUtils.isEmpty(resultHtml)) {
            return resultHtml;
        }

        String patcherCharset;
        Pattern pattern = Pattern.compile("(?i)(GB2312|GBK|UTF-8)");
        Matcher matcher = pattern.matcher(resultHtml);

        if (matcher.find()) {
            patcherCharset = matcher.group();
        } else {
            patcherCharset = defaultCharset;
        }

        // 如果两个编码不一样，重新编码
        if (!patcherCharset.equalsIgnoreCase(defaultCharset)) {
            try {
                if ("GB2312".equalsIgnoreCase(patcherCharset)) {
                    resultHtml = new String(resultHtml.getBytes("gbk"), patcherCharset);
                } else {
                    resultHtml = new String(resultHtml.getBytes("iso-8859-1"), patcherCharset);
                }
//                resultHtml = new String(resultHtml.getBytes(), patcherCharset);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        if (StringUtils.isEmpty(resultHtml)) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("未抓取到内容! pageUrl {} resultHtml {}", url, resultHtml);
            }
            return null;
        }
        return resultHtml;
    }

    private String request(String url, String charset) {
        CloseableHttpResponse response = null;
        try {
            Map<String, String> headers = getHeaders();
            response = Http2Util.httpGetWithHttpResponse(url, null, headers);
            int statusCode = response.getStatusLine().getStatusCode();

            // TODO: 2018/9/7 404
            if (statusCode != CodeConstants.HTTP_SUCCESS_200) {
                return null;
            }
            return EntityUtils.toString(response.getEntity(), charset);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            HttpUtil.closeQuietly(response);
        }
    }

    private Map<String, String> getHeaders() {
        return ImmutableMap.of(
                "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
                "User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36",
                "Accept-Encoding", "gzip, deflate, br",
                "Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8"
        );
    }

    private String buildUrl(String url, String pageUrl) {
        String newPageListUrl = null;
        if (url.startsWith("http")) {
            newPageListUrl = url;
        } else {
            // ./nbs.D110000renmrb_01.htm   nbs.D110000renmrb_02.htm
            if (url.startsWith("./")) {
                url = url.replace("./", "");
            }

            if (url.startsWith("/")) {
                try {
                    URL url2 = new URL(pageUrl);
                    String host = url2.getHost();
                    String protocol = url2.getProtocol();
                    newPageListUrl = protocol + "//" + host  + "/" + url;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            } else {
                // 处理上面两种情况
                newPageListUrl = getUrlPrefix(pageUrl) + url;
            }
        }
        return newPageListUrl;
    }

    private String getUrlPrefix(String url) {
        String factoryPageUrl = PaperUrlFactory.factory(url);
        int endIndex = factoryPageUrl.lastIndexOf("/");
        String urlPrefix = factoryPageUrl.substring(0, endIndex + 1);
        return urlPrefix;
    }

//    private String request(String url) {
//        url = "http://paper.people.com.cn/rmrb";
//        CloseableHttpResponse response = null;
//
//        CloseableHttpClient httpclient = HttpClients.custom()
//                .setRedirectStrategy(new LaxRedirectStrategy())
//                .build();
//        try {
//
//            HttpClientContext context = HttpClientContext.create();
//            HttpGet httpGet = new HttpGet(url);
//            System.out.println("Executing request " + httpGet.getRequestLine());
//            System.out.println("----------------------------------------");
//
//            httpclient.execute(httpGet, context);
//            HttpHost target = context.getTargetHost();
//            List<URI> redirectLocations = context.getRedirectLocations();
//            URI location = URIUtils.resolve(httpGet.getURI(), target, redirectLocations);
//            System.out.println("Final HTTP location: " + location.toASCIIString());
//
//            return null;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        } finally {
//            HttpUtil.closeQuietly(response);
//        }
//    }
}