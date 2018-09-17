package cn.mc.scheduler.util;

import cn.mc.core.dataObject.SystemKeywordsDO;
import cn.mc.core.dataObject.logs.KeywordsLogsDO;
import cn.mc.core.utils.BeanManager;
import cn.mc.core.utils.IDUtil;
import cn.mc.core.utils.MD5Util;
import cn.mc.scheduler.mapper.KeywordsLogsMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Selectable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 抓取新闻工具类
 *
 * @author daiqingwen
 * @date 2018-6-12 下午 17:35
 */
@Component
public class SchedulerUtils {

    private static Logger log = LoggerFactory.getLogger(SchedulerUtils.class);

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private KeywordsLogsMapper keywordsLogsMapper;

    private String replace = "replaceKeywords";

    private String matcher = "matcherKeywords";

    //获取配置的正则表达式
    private static String REGXPFORTAG = "<\\s*img\\s+([^>]*)\\s*";
    //获取配置的正则表达式
    private static String REGXPFORTAGATTRIB = "src=\\s*\"([^\"]+)\"";

    /**
     * 截取关键字
     *
     * @param keyword 关键字
     * @return keywords
     */
    public String subStringKeyword(String keyword) {
        if (StringUtils.isEmpty(keyword)) {
            return keyword;
        }
        String keywords = "";
        StringBuilder str = new StringBuilder(keyword);
        if (str.toString().length() >= 500) {
            keywords = str.toString().substring(0, 500);
        } else {
            keywords = str.toString();
        }
        return keywords;
    }

    /**
     * 关键字匹配，匹配成功则返回true
     *
     * @param content 内容
     * @param type    0 新闻内容 1 新闻来源 2 MD5 3 标题
     * @return boolean
     */
    public boolean keywordsMatch(String content, Integer type, Long newsId) {
        boolean flag = true;
        // 查询需要跳过的关键字
        String data = get(matcher);
        if (StringUtils.isEmpty(data)) {
            return false;
        }
        JSONArray json = (JSONArray) JSONArray.parse(data);
        for (int i = 0; i < json.size(); i++) {
            JSONObject obj = json.getJSONObject(i);
            // 内容类型 0 新闻内容 1 新闻来源 2 MD5 3 标题
            Integer contentType = obj.getInteger("contentType");
            // 验证方式 0 关键字 1 正则表达式
            Integer validateType = obj.getInteger("validateType");
            String keywords = obj.getString("keywords");
            if (StringUtils.isEmpty(keywords)) {
                continue;
            }
            if (type.equals(contentType)) {
                if (!matcher(validateType, keywords, content, newsId, contentType)) {
                    flag = false;
                } else {
                    return true;
                }
            }

        }
        return flag;
    }

    private boolean matcher(Integer validateType, String keywords, String content, Long newsId, Integer contentType) {
        boolean flag = true;
        String rgx = "[\r\n]";
        String[] keys = keywords.split(rgx);
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            if (StringUtils.isEmpty(key)) {
                continue;
            }
            if (SystemKeywordsDO.REGEX.equals(validateType)) {
                Pattern p = Pattern.compile(key);
                Matcher matcher = p.matcher(content);
                if (!matcher.find()) {
                    flag = false;
                } else {
                    content = matcher.group();
                    // 新增日志
                    KeywordsLogsDO logsDO = new KeywordsLogsDO(IDUtil.getNewID(), newsId, key, content, contentType, validateType);
                    keywordsLogsMapper.insert(logsDO);
                }
            } else {
                if (!content.contains(key)) {
                    flag = false;
                } else {
                    // 新增日志
                    KeywordsLogsDO logsDO = new KeywordsLogsDO(IDUtil.getNewID(), newsId, key, key, contentType, validateType);
                    keywordsLogsMapper.insert(logsDO);
                }
            }
        }

        return flag;
    }

    /**
     * 关键字匹配，将匹配成功的字符替换成自定义的字符
     *
     * @param content 内容
     * @param type    0 新闻内容 1 新闻来源 2 MD5 3 标题
     * @param newsId  新闻id
     * @return String
     */
    public String keywordsReplace(String content, Integer type, Long newsId) {
        StringBuilder sb = new StringBuilder();
        // 查询需要替换的关键字
        String data = get(replace);
        if (StringUtils.isEmpty(data)) {
            sb.append(content);
            return sb.toString();
        }
        JSONArray json = (JSONArray) JSONArray.parse(data);
        for (int i = 0; i < json.size(); i++) {
            JSONObject obj = json.getJSONObject(i);
            Integer validateType = obj.getInteger("validateType");
            String keywords = obj.getString("keywords");
            String replaceContent = obj.getString("replaceContent");
            Integer contentType = obj.getInteger("contentType");
            if (StringUtils.isEmpty(keywords)) {
                continue;
            }

            if (type.equals(contentType)) {
                content = replace(validateType, keywords, content, replaceContent, contentType, newsId);
            }
        }
        sb.append(content);
        return sb.toString();
    }

    /**
     * MD5图片处理
     *
     * @param content 内容
     * @param newsId  新闻id
     * @return String
     */
    public String keywordsReplaceMd5(String content, Long newsId) {
        StringBuilder sb = new StringBuilder();
        // 查询需要替换的关键字
        String data = get(replace);
        if (StringUtils.isEmpty(data)) {
            sb.append(content);
            return sb.toString();
        }
        JSONArray json = (JSONArray) JSONArray.parse(data);
        for (int i = 0; i < json.size(); i++) {
            JSONObject obj = json.getJSONObject(i);
            //校验类型
            Integer validateType = obj.getInteger("validateType");
            //关键字
            String keywords = obj.getString("keywords");
            //被替换的内容
            String replaceContent = obj.getString("replaceContent");
            //校验类型 这个是默认是md5可以写死
            Integer contentType = obj.getInteger("contentType");
            if (StringUtils.isEmpty(keywords)) {
                continue;
            }
            //是图片MD5的值
            if (contentType == 2) {
                String rgx = "[\r\n]";
                String[] keys = keywords.split(rgx);
                for (int j = 0; j < keys.length; j++) {
                    String key = keys[j];
                    if (StringUtils.isEmpty(key)) {
                        continue;
                    }
                    //如果校验到了Md5的值
                    if (content.contains(key)) {
                        content = content.replace(key, (StringUtils.isEmpty(replaceContent) ? "" : replaceContent));
                        // 新增日志
                        KeywordsLogsDO logsDO = new KeywordsLogsDO(IDUtil.getNewID(), newsId, key, key, contentType, validateType);
                        keywordsLogsMapper.insert(logsDO);
                    }
                }
                content = replace(validateType, keywords, content, replaceContent, contentType, newsId);
            }
        }
        sb.append(content);
        return sb.toString();
    }

    private String replace(Integer validateType, String keywords, String content, String replaceContent, Integer contentType, Long newsId) {
        String rgx = "[\r\n]";
        String[] keys = keywords.split(rgx);
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            if (StringUtils.isEmpty(key)) {
                continue;
            }
            if (SystemKeywordsDO.REGEX.equals(validateType)) {
                Pattern p = Pattern.compile(key);
                Matcher matcher = p.matcher(content);
                if (matcher.find()) {
                    String m = matcher.group();
                    content = matcher.replaceAll((StringUtils.isEmpty(replaceContent) ? "" : replaceContent));
                    // 新增日志
                    KeywordsLogsDO logsDO = new KeywordsLogsDO(IDUtil.getNewID(), newsId, key, m, contentType, validateType);
                    keywordsLogsMapper.insert(logsDO);
                }
            } else {
                if (content.contains(key)) {
                    content = content.replace(key, (StringUtils.isEmpty(replaceContent) ? "" : replaceContent));
                    // 新增日志
                    KeywordsLogsDO logsDO = new KeywordsLogsDO(IDUtil.getNewID(), newsId, key, key, contentType, validateType);
                    keywordsLogsMapper.insert(logsDO);
                }
            }

        }
        return content;
    }

    /**
     * 获取redis数据
     *
     * @param key
     * @return
     */
    public String get(final String key) {
        String obj = (String) redisTemplate.opsForValue().get(key);
        return obj;
    }

    /**
     * 替换所有 空字符、回车、换行
     *
     * @param text
     * @return
     */
    public static String filterText(String text) {
        if (StringUtils.isEmpty(text)) {
            return text;
        }
        return text
                .replaceAll("\\s|\n|\r|\t", "")
                // 特殊空格
                .replaceAll("　", "")
                .replaceAll("null", "");
    }

    /**
     * 过滤 html 内容
     *
     *  <p>
     *      所有没用的样式，script a class style 等属性过滤
     *  </p>
     *
     * @param html
     * @return
     */
    public static String filterHtmlLabel(String html) {
        return HtmlNodeUtil.htmlFormat(html);
    }

    /**
     * 内容过滤
     *
     * <strong>
     * <p>
     * 任何操作失败 返回 null，成功返回新的 content
     *
     * </strong>
     *
     * @param content
     * @param source
     * @return 任何操作失败 返回 null，成功返回新的 content
     */
    public static String contentFilter(@NotNull String content,
                                       @NotNull String source,
                                       @NotNull String title,
                                       @NotNull Long newsId) {

        SchedulerUtils schedulerUtils = BeanManager.getBean(SchedulerUtils.class);

        // 替换内容
        String replaceContent = schedulerUtils
                .keywordsReplace(content, SystemKeywordsDO.CONTENT_TYPE_TEXT, newsId);

        if (StringUtils.isEmpty(replaceContent)) {
            return null;
        }

        // 过滤内容
        boolean contentMatch = schedulerUtils.keywordsMatch(
                content, SystemKeywordsDO.CONTENT_TYPE_TEXT, newsId);

        if (contentMatch) {
            return null;
        }

        // 替换内容
        boolean sourceMatch = schedulerUtils.keywordsMatch(
                source, SystemKeywordsDO.CONTENT_TYPE_SOURCE, newsId);

        if (sourceMatch) {
            return null;
        }

        // title 来源
        boolean titleMatch = schedulerUtils.keywordsMatch(
                title, SystemKeywordsDO.CONTENT_TYPE_TITLE, newsId);

        if (titleMatch) {
            return null;
        }

        // 检查内容长度，去掉 html 标签检查
        replaceContent = replaceContent
                .replaceAll("\n|\r", "")
                .trim();

        int contentLength = Html.create(replaceContent)
                .xpath("//allText()")
                .toString()
                .replaceAll("\n|\r", "")
                .length();

        // 小于 20 个字符的新闻不要
        if (contentLength < 20) {
            return null;
        }

        // 去掉所有 a 链接
        replaceContent = filterHtmlLabel(replaceContent);

        //存储对象
        HashMap<String, String> map = new HashMap<>();

        //把文章的src替换成md5值
        replaceContent = replaceMD5Img(replaceContent, map);

        //调用替换md5值的内容
        replaceContent = schedulerUtils
                .keywordsReplace(replaceContent, 2, newsId);

        //把控制的MD5值去掉
        replaceContent = replaceSrcISNull(replaceContent);

        //把所有的未过滤的md5值转换成对象的图片对象
        for (String keys : map.keySet()) {
            String values = map.get(keys);
            replaceContent = replaceContent.replaceAll(values, keys);
        }

        //去掉body
        if (replaceContent.contains("<body>")) {
            replaceContent = replaceContent.replaceAll("<body>", "");
            replaceContent = replaceContent.replaceAll("</body>", "");
        }

        //清除缓存对象
        map.clear();
        return replaceContent;
    }

    //把src的内容替换成md5对象
    private static String replaceMD5Img(String replaceContent, HashMap map) {
//        if (true) {
//            return replaceContent;
//        }
        Html html = new Html(replaceContent);
        List<Selectable> nodes = html.xpath("//body").nodes();
        Selectable articleNode = nodes.get(0);
        List<Selectable> imgNodes = articleNode.xpath("//img").nodes();
        for (Selectable imgNode : imgNodes) {
            Element elements = HtmlNodeUtil.getElements((HtmlNode) imgNode).get(0);
            String dataSrc = elements.attr("src");
            String imgFilterUrl = new String(dataSrc.replaceAll("amp;", "").trim());
            if (!StringUtils.isEmpty(imgFilterUrl)) {

                // 补充前缀 http 包含 https
                if (!imgFilterUrl.startsWith("http")) {
                    if (imgFilterUrl.startsWith("//")) {
                        // 判断 cdn 路径
                        imgFilterUrl = "http:" + imgFilterUrl;
                    } else {
                        // 普通路径
                        imgFilterUrl = "http://" + imgFilterUrl;
                    }
                }
                try {
                    InputStream inputStream = ImgUtil.getInputStream(imgFilterUrl);
                    if (inputStream == null) {
                        continue;
                    }

                    //将InputStream对象转换成ByteArrayOutputStream
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) > -1) {
                        byteArrayOutputStream.write(buffer, 0, len);
                    }
                    byteArrayOutputStream.flush();
                    //将byteArrayOutputStream可转换成多个InputStream对象，达到多次读取InputStream效果
                    InputStream inputStreamA = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                    InputStream inputStreamB = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                    //获取图片MD5值
                    String imgMD5 = MD5Util.getMD5(inputStreamA);
                    BufferedImage sourceImg = ImageIO.read(inputStreamB);
                    if (null != sourceImg) {
                        //如果宽度和高度小于5则直接移除此图片
                        if (sourceImg.getWidth() <= 5 && sourceImg.getHeight() <= 5) {
                            elements.remove();
                            continue;
                        }
                    }

                    map.put(imgFilterUrl, imgMD5);
                    elements.attr("src", imgMD5);
                } catch (IOException e) {
//                    log.error("md5 替换异常! \n {}", ExceptionUtils.getStackTrace(e));
//                    return replaceContent;
                    // skip
                }
            }
        }
        return articleNode.toString();
    }

    private static String replaceSrcISNull(String content) {
        Html html = new Html(content);
        List<Selectable> nodes = html.xpath("//body").nodes();
        Selectable articleNode = nodes.get(0);
        List<Selectable> imgNodes = articleNode.xpath("//img").nodes();
        for (Selectable imgNode : imgNodes) {
            Element elements = HtmlNodeUtil.getElements((HtmlNode) imgNode).get(0);
            String dataSrc = elements.attr("src");
            String imgFilterUrl = new String(dataSrc.replaceAll("amp;", "").trim());
            if (StringUtils.isEmpty(imgFilterUrl)) {
                elements.remove();
            }
        }
        return articleNode.toString();
    }

    /**
     * 替换所有<a>标签
     *
     * @param content
     * @return String
     */
    public String replaceLabel(String content) {
        String reg = "<a[^>]+>([^<]+)</a>";
        Pattern p = Pattern.compile(reg);
        Matcher m = p.matcher(content);
        while (m.find()) {
            content = m.replaceAll("");
        }
        return content;
    }

    /**
     * 获取图片尺寸
     *
     * @param imgUrl
     * @return
     * @throws IOException
     */
    public Map<String, Integer> parseImg(String imgUrl) {
        if (StringUtils.isEmpty(imgUrl)) {
            return null;
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(imgUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setConnectTimeout(1000 * 10);
            connection.setReadTimeout(1000 * 10);

            BufferedImage image = ImageIO.read(connection.getInputStream());
            int srcWidth = image.getWidth();      // 源图宽度
            int srcHeight = image.getHeight();    // 源图高度
            Map<String, Integer> resultMap = Maps.newHashMap();
            resultMap.put("width", srcWidth);
            resultMap.put("height", srcHeight);
            return resultMap;
        } catch (Exception e) {
            // skip
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 获取缓存数据做对比
     *
     * @param cache 缓存数据
     * @param key
     */
    public boolean getCacheData(String cache, String key) {
        JSONArray cacheArray = JSON.parseArray(cache);
        boolean flag = false;
        for (int j = 0; j < cacheArray.size(); j++) {
            JSONObject jObject = cacheArray.getJSONObject(j);
            String datakey = jObject.getString("dataKey");
            if (datakey.equals(key)) {
                flag = true;
                break;
            }
        }
        return flag;
    }
}
