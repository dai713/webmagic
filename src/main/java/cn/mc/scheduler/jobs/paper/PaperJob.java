//package cn.mc.scheduler.jobs.paper;
//
//import cn.mc.core.dataObject.paper.PaperLogDO;
//import cn.mc.core.dataObject.paper.PaperDO;
//import cn.mc.core.dataObject.paper.PaperFilterRegexDO;
//import cn.mc.core.dataObject.paper.PaperNewsDO;
//import cn.mc.core.exception.SchedulerNewException;
//import cn.mc.core.mybatis.Field;
//import cn.mc.core.mybatis.Update;
//import cn.mc.scheduler.base.BaseJob;
//import cn.mc.scheduler.mapper.GrabPaperLogMapper;
//import cn.mc.scheduler.mapper.PaperMapper;
//import cn.mc.scheduler.mapper.PaperNewsMapper;
//import cn.mc.scheduler.util.HttpUtils;
//import cn.mc.scheduler.util.ImageUtils;
//import cn.mc.scheduler.util.UrlUtils;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//
//import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//@Component
//public class PaperJob extends BaseJob {
//
//    @Autowired
//    private PaperMapper paperMapper;
//
//    @Autowired
//    private GrabPaperLogMapper grabPaperLogMapper;
//
//    @Autowired
//    private PaperNewsMapper paperNewsMapper;
//
//    private static List<PaperFilterRegexDO> titleRegexList = null, contentRegexList = null;
//
//    /**
//     * 每天2点执行根据日志统计新闻机器审核数据
//     */
//    @Override
////    @Scheduled(cron = "0 0 */1 * * *")
//    public void execute() throws SchedulerNewException {
//        titleRegexList = paperNewsMapper.selectPaperFilterRegexByType(0, new Field());//标题正则过滤列表
//        contentRegexList = paperNewsMapper.selectPaperFilterRegexByType(1, new Field());//标题正则过滤列表
//        work();
//    }
//
//
//    public void work() {
//        try {
//            /**
//             * 抓取报纸数据
//             */
//            List<PaperDO> paperList = paperMapper.selectAllList(new Field());
//            for (PaperDO paperDo : paperList) {
//                if (!grab(paperDo)) {
////                    System.exit(1);
//                    continue;
//                }
//            }
//            System.out.println("==========报纸新闻抓取结束===========");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    /**
//     * 抓取报纸新闻
//     *
//     * @param paperDO
//     * @return
//     */
//    public boolean grab(PaperDO paperDO) {
//        boolean resultFlag = true;
//        try {
//            if (chechedMatcherIsNull(paperDO)) {
//                String domain = paperDO.getUrl();
//
//                /**
//                 * 验证链接报纸网站是否正常
//                 */
//                String[] results = HttpUtils.sendGets(domain, "", paperDO.getCharset());
//                String resJson = results[1];
//                if (StringUtils.isEmpty(resJson)) {
//                    PaperLogDO grabPaperLogDO = new PaperLogDO();
//                    grabPaperLogDO.setUrl(domain);
//                    updatePaperByGrapStatus(paperDO.getId(), 5, grabPaperLogDO);//连接失败
//                    return false;
//                }
//
//                /**
//                 * 报纸网站地址需要获取最新重定向地址
//                 */
//                if (!startGrab(resJson, results[0], paperDO)) {
//                    return false;
//                }
//            } else {
//                Long paperId = paperDO.getId();
//                PaperLogDO grabPaperLogDO = new PaperLogDO();
//                grabPaperLogDO.setMessage("正则表达式存在空值");
//                updatePaperByGrapStatus(paperId, 1, grabPaperLogDO);//正则匹配失败,抓取异常
//                return false;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return resultFlag;
//    }
//
//    /**
//     * 检查是否html meta重定向
//     *
//     * @param resHtml
//     * @return
//     */
//    private boolean startGrab(String resHtml, String domain, PaperDO paperDO) {
//        boolean returnFlag = true;
//        String resJson = resHtml;
//        String redirectUrl = domain;
//        try {
//
//            /**
//             * 获取版面url地址
//             */
//            String pageMatcher = paperDO.getPageMatcher();
//            String[] matchers = pageMatcher.split(",");
//            if (matchers.length > 1) {
//                pageMatcher = matchers[1];
//                String domainMatcher = matchers[0];
//                Document pageDoc = Jsoup.parse(resJson);
//                Element domainElement = pageDoc.select(domainMatcher).first();
//                String domainUrl = domainElement.attr("href");
//                if (StringUtils.isEmpty(domainUrl)) {
//                    domainUrl = domainElement.attr("src");
//                }
//                domainUrl = UrlUtils.buildUrl(domain, domainUrl);
//                redirectUrl = domainUrl;
//                resJson = HttpUtils.sendGet(domainUrl, "", paperDO.getCharset());
//            }
//
//            List<String> pageUrlList = new ArrayList<>();
//            Document pageDoc = Jsoup.parse(resJson);
//            Elements elementsA = pageDoc.select(pageMatcher);
//            for (Element ele : elementsA) {
//                String pageUrl = ele.attr("href");
//
//                String tempUrl = "";
//                if (pageUrl.lastIndexOf(".") > -1) {
//                    tempUrl = pageUrl.substring(pageUrl.lastIndexOf("."));
//                }
//                if (!tempUrl.contains("pdf") && !tempUrl.contains("PDF")) {
//                    changeListUrl(pageUrlList, pageUrl, redirectUrl);
//                }
//            }
//
//            if (pageUrlList.size() == 0) {
//                pageGrabFail(paperDO.getId(), redirectUrl);
//                return false;
//            }
//
//            /**
//             * 抓取标题数据
//             */
//            for (String pageUrl : pageUrlList) {
//                System.out.println("pageUrl：" + pageUrl);
//                grapTitleUrl(pageUrl, paperDO);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return returnFlag;
//    }
//
//    /**
//     * 版面抓取失败
//     *
//     * @param paperId
//     * @param redirectUrl
//     */
//    private void pageGrabFail(Long paperId, String redirectUrl) {
//        PaperLogDO grabPaperLogDO = new PaperLogDO();
//        grabPaperLogDO.setUrl(redirectUrl);
//        grabPaperLogDO.setMessage(redirectUrl + "重定向地址获取获取数据失败");
//        updatePaperByGrapStatus(paperId, 2, grabPaperLogDO);//获取重定向地址失败
//    }
//
//    /**
//     * 抓取报纸标题
//     *
//     * @param pageUrl
//     * @param paperDO
//     * @return
//     */
//    private boolean grapTitleUrl(String pageUrl, PaperDO paperDO) {
//        boolean resultFlag = true;
//        try {
//            String resJson = HttpUtils.sendGet(pageUrl, "", paperDO.getCharset());
//
//            /**
//             * 验证请求版面url是否正常
//             */
//            if (StringUtils.isEmpty(resJson)) {
//                titleGrabFail(paperDO.getId(), pageUrl);
//                return false;
//            }
//
//            /**
//             * 获取标题链接地址
//             */
//            List<Map<String, Object>> titleUrlList = new ArrayList<>();
//            /**
//             * 获取标题链接地址
//             */
//            String titleMatcher = paperDO.getTitleMatcher();
//            String[] matchers = titleMatcher.split(",");
//            Document doc = Jsoup.parse(resJson);
//            if (matchers.length == 1) {
//                Elements eles = doc.select(titleMatcher);
//                for (Element element : eles) {
//                    String titleUrl = element.attr("href");
//                    String title = element.text();
//                    if (StringUtils.isEmpty(title)) {
//                        title = element.attr("msg");
//                    }
//                    if (StringUtils.isEmpty(title)) {
//                        title = matcherScriptText(element.outerHtml());
//                    }
//                    if (element.tagName().equalsIgnoreCase("area")) {
//                        title = element.attr("name");
//                    }
//                    if (!StringUtils.isEmpty(title)) {
//                        changeListUrl(titleUrlList, titleUrl, pageUrl, title);
//                    }
//
//                }
//            } else {
//                titleMatcher = matchers[1];
//                Elements eles = doc.select(titleMatcher);
//                for (Element element : eles) {
//                    String titleUrl = UrlUtils.titleUrl(matchers[0], element.outerHtml());
//                    String title = element.text();
//                    if (StringUtils.isEmpty(title)) {
//                        title = element.attr("msg");
//                    }
//                    if (StringUtils.isEmpty(title)) {
//                        title = matcherScriptText(element.outerHtml());
//                    }
//                    if (element.tagName().equalsIgnoreCase("area")) {
//                        title = element.attr("name");
//                    }
//                    if (!StringUtils.isEmpty(title)) {
//                        changeListUrl(titleUrlList, titleUrl, pageUrl, title);
//                    }
//
//                }
//            }
//
//            if (titleUrlList.size() == 0) {
//                titleGrabFail(paperDO.getId(), pageUrl);
//                return false;
//            }
//
//            /**
//             * 抓取内容
//             */
//            for (Map<String, Object> map : titleUrlList) {
//                String titleUrl = map.get("url").toString();
//                String title = map.get("title").toString();
//                if (!newsIsExists(titleUrl)) {
//                    grapContent(titleUrl, title, paperDO);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return resultFlag;
//    }
//
//    /**
//     * 正则处理js新闻标题文本
//     *
//     * @param html
//     * @return
//     */
//    private static String matcherScriptText(String html) {
//        String result = "";
//        String regex = "(<script>.*?(\"|\'))(.*?)((\"|\').*?</script>)";
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(html);
//        while (matcher.find()) {
//            result = matcher.group(3);
//            break;
//        }
//        return result;
//    }
//
//    /**
//     * 标题抓取失败
//     *
//     * @param paperId
//     * @param pageUrl
//     */
//    private void titleGrabFail(Long paperId, String pageUrl) {
//        PaperLogDO grabPaperLogDO = new PaperLogDO();
//        grabPaperLogDO.setUrl(pageUrl);
//        grabPaperLogDO.setMessage(pageUrl + "版面地址连接获取数据失败");
//        updatePaperByGrapStatus(paperId, 3, grabPaperLogDO);//版面地址连接失败
//    }
//
//    /**
//     * 抓取报纸内容
//     *
//     * @param titleUrl
//     * @param paperDO
//     * @return
//     */
//    private boolean grapContent(String titleUrl, String title, PaperDO paperDO) {
//        boolean resultFlag = true;
//        try {
//            String resJson = HttpUtils.sendGet(titleUrl, "", paperDO.getCharset());
//
//            /**
//             * 验证标题地址是否正常
//             */
//            if (StringUtils.isEmpty(resJson)) {
//                PaperLogDO grabPaperLogDO = new PaperLogDO();
//                grabPaperLogDO.setUrl(titleUrl);
//                grabPaperLogDO.setMessage(titleUrl + "标题地址连接获取数据失败");
//                updatePaperByGrapStatus(paperDO.getId(), 4, grabPaperLogDO);//标题地址连接失败
//                return false;
//            }
//
//            /**
//             * 根据内容正则获取内容数据
//             */
//            String contentMatcher = paperDO.getContentMatcher();
//            String matchers[] = contentMatcher.split(",");
//            String contentHtml = "";
//
//            Document doc = Jsoup.parse(resJson);
//            Map<String, Object> imageMap = null;
//            Map<String, Object> contentMap = null;
//            for (int i = 0; i < matchers.length; i++) {
//                if (matchers.length > 1) {
//                    if (i == 0) {
//                        imageMap = matcherImgHtml(doc.select(matchers[i]), titleUrl);
//                    } else if (i == 1) {
//                        contentMap = matcherContentHtml(doc.select(matchers[i]), titleUrl);
//                    }
//                } else {
//                    contentMap = matcherContentHtml(doc.select(contentMatcher), titleUrl);
//                }
//
//            }
//
//            String imgHtml = "";
//            if (imageMap != null) {
//                imgHtml = imageMap.get("content").toString();
//            }
//
//            contentHtml = "";
//            if (contentMap != null) {
//                contentHtml = contentMap.get("content").toString();
//            }
//
//            String content = imgHtml + contentHtml;
//            content = filterContentRegex(content);//正则过滤内容
//
//            if (!StringUtils.isEmpty(Jsoup.parse(content).text().trim()) || !StringUtils.isEmpty(imgHtml)) {
//                int imgStatus = 0;
//                int contentStatus = 0;
//                if (matchers.length > 1) {
//                    imgStatus = Integer.parseInt(imageMap.get("status").toString());
//                    contentStatus = Integer.parseInt(contentMap.get("status").toString());
//                } else {
//                    contentStatus = Integer.parseInt(contentMap.get("status").toString());
//                }
//
//                if (imgStatus == 1 || contentStatus == 1) {
//                    System.out.println("====调试");
//                }
//                PaperNewsDO temp = paperNewsMapper.selectDataUrlByStatus(titleUrl, 2, new Field());
//                if (temp == null) {
//                    if (imgStatus == 0 && contentStatus == 0) {
//                        savePaperNews(paperDO.getId(), title, content, titleUrl, 0);
//                    } else {
//                        savePaperNews(paperDO.getId(), title, content, titleUrl, 2);
//                    }
//                } else {//更新操作
//                    if (imgStatus == 0 && contentStatus == 0) {
//                        paperNewsMapper.updateById(temp.getId(), Update.update("`status`", 0));
//                    } else {
//                        paperNewsMapper.updateById(temp.getId(), Update.update("`status`", 1));
//                    }
//                    paperNewsMapper.updateContentById(temp.getId(), Update.update("content", content));//修改内容
//                }
//
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("===============抓取内容异常===========");
//            System.out.println("paperId：" + paperDO.getId() + " title：" + title);
//            System.out.println("url:" + titleUrl);
//            System.out.println("===============抓取内容异常结束===========");
//            System.out.println();
//        }
//        return resultFlag;
//    }
//
//    private Map<String, Object> matcherContentHtml(Elements elementContent, String baseUrl) {
//        Map<String, Object> map = matcherImgHtml(elementContent, baseUrl);
//        String result = elementContent.outerHtml();
//        if (".xml".equals(UrlUtils.urlSuffix(baseUrl))) {
//            result = elementContent.text();
//        }
//        if (StringUtils.isEmpty(elementContent.text().trim())) {
//            result = "";
//        }
//        map.put("content", result);
//        return map;
//    }
//
//    private Map<String, Object> matcherImgHtml(Elements elementContent, String baseUrl) {
//        Map<String, Object> map = new HashMap<>();
//        map.put("status", 0);
//        String content = "";
//        try {
//            removeElementAhref(elementContent.select("a"), "href");
//            Elements imgs = elementContent.select("img");
//            int imgCount = imgs.size();
//            for (Element element : imgs) {
//                String imgUrl = element.attr("src");
//                imgUrl = imgUrl.replaceAll("^\\*.*?\\[Article\\].*?\\*$", "");
//                if (!StringUtils.isEmpty(imgUrl)) {
//                    imgUrl = UrlUtils.buildUrl(baseUrl, imgUrl);
//                    imgUrl = UrlUtils.urlToUtf8(imgUrl);
//
//                    String fileUrl = ImageUtils.downloadImage(imgUrl);
//                    if (!StringUtils.isEmpty(fileUrl)) {
//                        element.attr("src", fileUrl);
//                    } else {
//                        element.remove();
//                        imgCount--;
//                        map.put("status", 1);
//                    }
//                } else {
//                    element.remove();
//                    imgCount--;
//                }
//
//            }
//            if (imgs.size() > 0) {
//                content = elementContent.outerHtml();
//            }
//            if (imgCount <= 0) {
//                content = "";
//            }
//
//            if (".xml".equals(UrlUtils.urlSuffix(baseUrl))) {
//                content = elementContent.text();
//            }
//        } catch (Exception e) {
//            System.out.println("图片内容抓取失败，内容地址：" + baseUrl);
//        } finally {
//            map.put("content", content);
//        }
//        return map;
//    }
//
//    private void removeElementAhref(Elements eles, String attr) {
//        for (Element element : eles) {
//            element.attr(attr, "javascript:;");
//        }
//    }
//
//    /**
//     * 保存标题新闻
//     *
//     * @param paperId
//     * @param title
//     * @param content
//     * @param dataUrl
//     */
//    private void savePaperNews(Long paperId, String title, String content, String dataUrl, int status) {
//        System.out.println();
//        System.out.println("paperId：" + paperId + " title：" + title);
//        PaperNewsDO paperNewsDO = new PaperNewsDO();
//        paperNewsDO.setPaperId(paperId);
//        paperNewsDO.setTitle(title);
//        paperNewsDO.setUrl(dataUrl);
//        paperNewsDO.setStatus(status);
//        paperNewsDO.setCreateTime(new Date());
//        paperNewsMapper.insert(paperNewsDO);
//        Long id = paperNewsDO.getId();
//        paperNewsMapper.updateById(id, Update.update("order_by", id));//同步排序号ID
//        paperNewsMapper.insertContent(Update.update("id", id).set("content", content));//保存内容
//    }
//
//    private boolean newsIsExists(String dataUrl) {
//        PaperNewsDO temp = paperNewsMapper.selectByDataUrl(dataUrl, new Field());
//        if (temp == null) {
//            return false;
//        } else {
//            return true;
//        }
//
//    }
//
//    /**
//     * 标题list数据
//     *
//     * @param list
//     * @param url
//     * @param startUrl
//     */
//    private void changeListUrl(List<Map<String, Object>> list, String url, String startUrl, String title) {
//        try {
//            title = filterTitleRegex(title);//正则过滤标题
//            if (!StringUtils.isEmpty(title)) {
//                url = UrlUtils.buildUrl(startUrl, url);
//                Map<String, Object> map = new HashMap<>();
//                map.put("url", url);
//                map.put("title", title);
//                list.add(map);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    /**
//     * 版面list数据
//     *
//     * @param list
//     * @param url
//     * @param startUrl
//     */
//    private void changeListUrl(List<String> list, String url, String startUrl) {
//        try {
//            url = UrlUtils.buildUrl(startUrl, url);
//            if (list.indexOf(url) == -1) {
//                list.add(url);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    /**
//     * 更新报纸抓取状态
//     *
//     * @param paperId
//     * @param status
//     */
//    private void updatePaperByGrapStatus(Long paperId, Integer status, PaperLogDO grabPaperLogDO) {
//        paperMapper.updateById(paperId, Update.update("grab_status", status));
//        grabPaperLogDO.setPaperId(paperId);
//        grabPaperLogDO.setCreateTime(new Date());
//        grabPaperLogMapper.insert(Update.copyWithoutNull(grabPaperLogDO));
//    }
//
//    /**
//     * 验证正则是否为空
//     *
//     * @param paperDo
//     * @return
//     */
//    private boolean chechedMatcherIsNull(PaperDO paperDo) {
//        boolean resultFlag = true;
//        try {
//            Long paperId = paperDo.getId();
//            String pageMatcher = paperDo.getPageMatcher();
//            String titleMatcher = paperDo.getTitleMatcher();
//            String contentMatcher = paperDo.getContentMatcher();
//
//            if (StringUtils.isEmpty(pageMatcher)) {
//                resultFlag = false;
//                matcherWarning(paperId, 0);
//                sendMatcherWarningMsg(0, paperDo);//发送异常信息
//            }
//            if (StringUtils.isEmpty(titleMatcher)) {
//                resultFlag = false;
//                matcherWarning(paperId, 1);
//                sendMatcherWarningMsg(1, paperDo);//发送异常信息
//            }
//            if (StringUtils.isEmpty(contentMatcher)) {
//                resultFlag = false;
//                matcherWarning(paperId, 2);
//                sendMatcherWarningMsg(2, paperDo);//发送异常信息
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return resultFlag;
//    }
//
//    /**
//     * 正则异常更新状态
//     *
//     * @param paperId
//     * @param type
//     */
//    private void matcherWarning(Long paperId, int type) {
//        String column = "";
//        if (type == 0) {//版面
//            column = "page_matcher_status";
//        } else if (type == 1) {//标题
//            column = "title_matcher_status";
//        } else if (type == 2) {//内容
//            column = "content_matcher_status";
//        }
//        paperMapper.updateById(paperId, Update.update(column, 1));
//    }
//
//    /**
//     * 发送异常信息
//     *
//     * @param type
//     * @param paperDO
//     */
//    private void sendMatcherWarningMsg(int type, PaperDO paperDO) {
//        try {
//            String paperName = paperDO.getName();
//            Long paperId = paperDO.getId();
//            if (type == 0) {//版面
//                System.out.println("id：" + paperId + "，报纸：" + paperName + "，版面正则匹配失败。");
//            } else if (type == 1) {//标题
//                System.out.println("id：" + paperId + "，报纸：" + paperName + "，标题正则匹配失败。");
//            } else if (type == 2) {//内容
//                System.out.println("id：" + paperId + "，报纸：" + paperName + "，内容正则匹配失败。");
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    /**
//     * 正则过滤标题
//     *
//     * @param html
//     * @return
//     */
//    public static String filterTitleRegex(String html) {
//        return filterRegex(titleRegexList, html);
//    }
//
//    /**
//     * 正则过滤内容
//     *
//     * @param html
//     * @return
//     */
//    public static String filterContentRegex(String html) {
//        return filterRegex(contentRegexList, html);
//    }
//
//    /**
//     * 正则过滤
//     *
//     * @param list
//     * @param html
//     * @return
//     */
//    public static String filterRegex(List<PaperFilterRegexDO> list, String html) {
//        for (PaperFilterRegexDO paperFilterRegex : list) {
//            String regex = paperFilterRegex.getRegex();
//            if (StringUtils.isEmpty(regex)) {
//                return html;
//            }
//            String replacement = paperFilterRegex.getReplacement();
//            if (StringUtils.isEmpty(replacement)) {
//                replacement = "";
//            }
//            Pattern pattern = Pattern.compile(regex);
//            Matcher matcher = pattern.matcher(html);
//            while (matcher.find()) {
//                html = html.replaceAll(Pattern.quote(matcher.group(1)), replacement);
//            }
//        }
//        return html;
//    }
//}
