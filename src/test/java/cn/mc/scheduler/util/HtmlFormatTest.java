package cn.mc.scheduler.util;

import com.google.common.collect.ImmutableList;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;

import java.util.List;

/**
 * @author Sin
 * @time 2018/9/14 下午5:45
 */
public class HtmlFormatTest {

    private static List<String> RETENTION_TAG_ATTR = ImmutableList.of("src");
    private static List<String> NOT_FILTER_TAG = ImmutableList.of("svg");
    private static List<String> REMOVE_TAG = ImmutableList.of("script", "style");
    private static Element recursionHtmlElement(Element element) {
        Element resultElements = new Element(element.tagName());
        for (Element childrenElement : element.children()) {

            String tagName = childrenElement.tagName();
            String text = childrenElement.ownText();

            // 删除标签
            if (REMOVE_TAG.contains(tagName)) {
                continue;
            }

            // 创建新节点
            Element newElement = new Element(tagName);
            newElement.text(text);

            // 开始过滤 attr，并设置到 newElement
            Attributes attributes = childrenElement.attributes();
            for (Attribute attribute : attributes) {
                String attrKey = attribute.getKey();
                String attrValue = attribute.getValue();

                // 需要过滤的标签，才进行属性过滤
                if (!NOT_FILTER_TAG.contains(tagName)) {
                    // 保留需要的 attr
                    if (!RETENTION_TAG_ATTR.contains(attrKey)) {
                        continue;
                    }
                }

                // 设置到 newElement
                newElement.attr(attrKey, attrValue);
            }

            // 检查是否还有 child
            if (childrenElement.children().size() > 0) {
                Element resultChildrenElement = recursionHtmlElement(childrenElement);
                resultElements.appendChild(resultChildrenElement);
            } else {
                resultElements.appendChild(newElement);
            }
        }
        return resultElements;
    }
}
