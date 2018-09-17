package cn.mc.scheduler.crawlerPaper;

import cn.mc.core.utils.DateUtil;
import com.google.common.collect.Lists;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 报纸 url factory
 *
 * @author Sin
 * @time 2018/9/4 下午8:26
 */
public class PaperUrlFactory {

    // http://paper.people.com.cn/rmrb/html/2018-09/04/nbs.D110000renmrb_01.htm
    // http://paper.people.com.cn/rmrb/html/{date,yyyy-MM}/{date,dd}/nbs.D110000renmrb_01.htm

    public static void main(String[] args) {
        PaperUrlFactory.factory("http://paper.people.com.cn/rmrb/html/{date,yyyy-MM}/{date,dd}/nbs.D110000renmrb_01.htm");
    }
    public static String factory(String url) {

        if (StringUtils.isEmpty(url)) {
            return url;
        }

        List<String> params = regexParams(url);
        for (String param : params) {
            String paramValue = handleParams(param);
            if (paramValue == null) {
                continue;
            }
            url = url.replace(param, paramValue);
        }
        return url;
    }

    public static String handleParams(String param) {
        param = param.substring(1, param.length() - 1);
        String[] paramInfo = param.split(",");
        String type = paramInfo[0];

        String result = null;
        switch (type) {
            case "date":
                result = handleDate(paramInfo[1]);
                break;
        }
        return result;
    }

    public static String handleDate(String format) {
        return DateUtil.format(DateUtil.currentDate(), format);
    }

    public static List<String> regexParams(String url) {
        String regex = "\\{.*?\\}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);

        List<String> result = Lists.newArrayList();
        while(matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }
}
