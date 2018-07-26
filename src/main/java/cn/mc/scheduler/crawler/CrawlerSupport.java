package cn.mc.scheduler.crawler;

import cn.mc.core.dataObject.NewsDO;
import cn.mc.core.dataObject.SystemKeywordsDO;
import cn.mc.core.utils.EncryptUtil;
import cn.mc.scheduler.util.SchedulerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.utils.FilePersistentBase;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * <p>
 *     匹配中文标点符号：
 *
 *     该表达式可以识别出： 。 ；  ， ： “ ”（ ） 、 ？ 《 》 这些标点符号。
 *     [\u3002\uff1b\uff0c\uff1a\u201c\u201d\uff08\uff09\u3001\uff1f\u300a\u300b]
 * </p>
 *
 *
 *  <p>
 *      匹配中文汉字: [\u4e00-\u9fa5]
 *  </p>
 *
 * @auther sin
 * @time 2018/3/7 14:27
 */
public abstract class CrawlerSupport extends FilePersistentBase {

    public static final String HEADER_CONTENT_TYPE_KEY = "content-type";
    public static final String HEADER_USER_AGENT_KEY = "User-Agent";

    public static final String USER_AGENT_IPHONE_OS = "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3 like Mac OS X) " +
            "AppleWebKit/602.1.50 (KHTML, like Gecko) CriOS/56.0.2924.75 Mobile/14E5239e Safari/602.1";

    public static final String NEED_SAVE_KEY = "NEED_SAVE";
    public static final String NEED_SAVE_YES = "YES";
    public static final String NEED_SAVE_NO = "NO";

    @Autowired
    private SchedulerUtils schedulerUtils;

    protected boolean isNeedSave(ResultItems resultItems) {
        if (NEED_SAVE_YES.equals(resultItems.get(NEED_SAVE_KEY)))
            return true;
        return false;
    }

    protected void setNeedSaveWithYes(Page page) {
        page.putField(NEED_SAVE_KEY, NEED_SAVE_YES);
    }


    protected String findRegex(String content, String regex) {
        Pattern pa = Pattern.compile(regex, Pattern.CANON_EQ);
        Matcher ma = pa.matcher(content);
        while (ma.find()) {
            return ma.group();
        }
        return "";
    }


    protected String clearNewLine(String str) {
        return str.replaceAll("\r\n", "")
                .replaceAll("\n", "");
    }

    protected String noTitleGoToArticleGetTitle(String article) {
        int titleLength = 20;
        String handleArticle = limitMaxCharacter(article, titleLength);

        String resultTitle = article.substring(0,
                firstPunctuationIndex(handleArticle, 0));

        return handleArticle.substring(0,
                firstPunctuationIndex(resultTitle, 0));
    }

    protected String noAbstractGoToArticleGetAbstract(String article) {
        int maxCharacter = 150;
        String limitArticle = limitMaxCharacter(article, maxCharacter);
        int lastIndex = limitArticle.length();

        int lastPunctuationIndex = lastPunctuationIndex(limitArticle, lastIndex);
        return limitArticle.substring(0, lastPunctuationIndex)  + "。";
    }

    private String limitMaxCharacter(String str, int maxSize) {
        int strLength = str.length();
        String result;
        if (strLength > maxSize) {
            result = str.substring(0, maxSize + 1);
        }
        else {
            result = str;
        }
        return result;
    }

    protected int firstPunctuationIndex(String str, int startIndex) {
        int firstStopIndex = str.indexOf("。", startIndex);
        if (firstStopIndex == -1) {
            int firstCommaIndex = str.indexOf("，");
            if (firstCommaIndex == -1) {
                return str.length();
            }
            else {
                return firstCommaIndex;
            }
        }
        else {
            return firstStopIndex;
        }
    }

    protected int lastPunctuationIndex(String str, int lastIndex) {
        int lastStopIndex = str.lastIndexOf("。", lastIndex);
        if (lastStopIndex == -1) {
            int lastCommaIndex = str.lastIndexOf("，");
            if (lastCommaIndex == -1) {
                return str.length();
            }
            else {
                return lastCommaIndex;
            }
        }
        else {
            return lastStopIndex;
        }
    }

    protected Map<String, String> analyticalUrlParams(String strUrl) {
        Map<String, String> mapRequest = new HashMap<>();

        String[] arrSplit;
        String strUrlParam = TruncateUrlPage(strUrl);
        if(strUrlParam == null) {
            return mapRequest;
        }

        arrSplit = strUrlParam.split("[&]");
        for(String strSplit:arrSplit) {
            String[] arrSplitEqual;
            arrSplitEqual = strSplit.split("[=]");

            if(arrSplitEqual.length > 1) {
                mapRequest.put(arrSplitEqual[0], arrSplitEqual[1]);
            } else {
                if(arrSplitEqual[0] != "") {
                    mapRequest.put(arrSplitEqual[0], "");
                }
            }
        }
        return mapRequest;
    }

    private static String TruncateUrlPage(String strUrl) {
        String strAllParam = null;
        String[] arrSplit = null;

        strUrl = strUrl.trim();

        arrSplit = strUrl.split("[?]");
        if(strUrl.length() > 1) {
            if(arrSplit.length > 1) {
                if(arrSplit[1] != null) {
                    strAllParam = arrSplit[1];
                }
            } else {
                strAllParam = strUrl;
            }
        }
        return strAllParam;
    }

    protected boolean isHtml(String string) {
        if (string.trim().indexOf("<!") != -1) {
            return true;
        }
        return false;
    }


    public static Integer handleVideoDisplayType() {
//        return (int)(Math.random() * 2) == 0 ?
//                NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE
//                : NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE;
        return NewsDO.DISPLAY_TYPE_ONE_LARGE_IMAGE;
    }

    public static Integer handleNewsDisplayType(Integer imageSize) {
        if (imageSize >= 3) {
            return NewsDO.DISPLAY_TYPE_THREE_MINI_IMAGE;
        } else if (imageSize >= 1) {
            return NewsDO.DISPLAY_TYPE_ONE_MINI_IMAGE;
        } else {
            return NewsDO.DISPLAY_TYPE_LINE_TEXT;
        }
    }

    private static final String REGEX_CLASS_STYLE =
            "((>)(&nbsp;)+)" +
            "|(onload=\\\".+\\\")" +
            "|(id=\"[^\"]+\")" +
            "|(class=\"[^\"]+\")" +
            "|(style=\"[^\"]+\")" +
            "|(<script.*?>.*?</script>)" +
            "|(点击图片看专题)" +
            "|(返回腾讯网首页>>)" +
            "|(返回腾讯网首页&gt;&gt;)" +
            "|(展开阅读全文)" +
            "|(查看更多)";

//    protected Selectable regexHtmlClassAndStyle(Selectable html) {
//        return html.replace(REGEX_CLASS_STYLE, " ");
//    }

//    @Deprecated
//    protected String regexHtmlClassAndStyleText(String html) {
//        if (StringUtils.isEmpty(html))
//            return "";
//
//        String content = schedulerUtils.keywordsReplace(
//                html, SystemKeywordsDO.CONTENT_TYPE_TEXT);
//        return content;
//    }
}
