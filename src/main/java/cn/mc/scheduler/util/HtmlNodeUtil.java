package cn.mc.scheduler.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import us.codecraft.webmagic.selector.HtmlNode;
import us.codecraft.webmagic.selector.Selectable;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author sin
 * @time 2018/6/23 17:31
 */
public class HtmlNodeUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlNodeUtil.class);

    /**
     * 获取 Element 节点
     *
     * <p>
     *     将 htmlNode 转换为 Element 节点属性
     * </p>
     *
     * @param htmlNode
     * @return Element 操作节点
     */
    public static List<Element> getElements(HtmlNode htmlNode) {
        Field field = ReflectionUtils.findField(htmlNode.getClass(), "elements");
        field.setAccessible(true);
        List<Element> elements = (List<Element>) ReflectionUtils.getField(field, htmlNode);
        return elements;
    }

    /**
     * 替换节点属性
     *
     *  <p>
     *      将 html 属性属性 设置到 另外一个属性
     *
     *      比如：爬虫经常遇见的图片 "懒加载" 图片在 data-src 中，
     *
     *      采用 String ReplaceAll 并不正确，会替换其他的 data-src
     *  </p>
     *
     * @param sourceAttr 原属性
     * @param targetAttr 目标属性
     * @param removeSourceAttr 设置后是否删除 sourceAttr
     * @param nodes 节点
     */
    public static void replaceNodeAttr(@NotNull List<Selectable> nodes,
                                       @NotNull String sourceAttr,
                                       @NotNull String targetAttr,
                                       @NotNull Boolean removeSourceAttr) {

        for (Selectable node : nodes) {
            List<Element> elementList = HtmlNodeUtil.getElements((HtmlNode) node);

            if (CollectionUtils.isEmpty(elementList)) {
                continue;
            }

            Element element = elementList.get(0);

            // 将 source 属性之，设置到 target 属性
            element.attr(targetAttr, element.attr(sourceAttr));
            if (removeSourceAttr) {
                // 是否删除 source 属性
                element.removeAttr(sourceAttr);
            }
        }
    }

    /**
     * 处理 cdn 路径
     *
     *  <p>
     *      cdn 路径已 "//" 开头，避免这种路径不能使用，
     *
     *      将处理为 http:// 前缀开头.
     *  </p>
     *
     * @param nodes 节点
     * @param attr 属性
     */
    public static void handleCdnUrl(
            @Nullable List<Selectable> nodes,
            @NotNull String attr) {

        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }

        for (Selectable imageNode : nodes) {
            List<Element> elementList = HtmlNodeUtil.getElements((HtmlNode) imageNode);
            if (!CollectionUtils.isEmpty(elementList)) {
                Element imageElement = elementList.get(0);
                String imageSrc = imageElement.attr(attr);
                if (StringUtils.startsWithIgnoreCase(imageSrc, "//")) {
                    imageSrc = "http:" + imageSrc;
                }

                // 重新设置 attr
                imageElement.attr(attr, imageSrc);
            }
        }
    }

    public static void handleCdnWithRemoveSourceAttr(
            @NotNull List<Selectable> nodes,
            @NotNull String sourceAttr,
            @NotNull String targetAttr) {

        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }

        // 替换属性，并且删除 sourceAttr 属性
        replaceNodeAttr(nodes, sourceAttr, targetAttr, true);

        // 处理图片 cdn 路径
        handleCdnUrl(nodes, targetAttr);
    }

    public static void removeLabel(@NotNull List<Selectable> nodes) {
        for (Selectable node : nodes) {
            List<Element> elements = getElements((HtmlNode) node);
            for (Element element : elements) {
                element.remove();
            }
        }
    }

    public static void replaceLabel(@NotNull List<Selectable> nodes) {
        for (Selectable node : nodes) {
            List<Element> elements = getElements((HtmlNode) node);
            if (CollectionUtils.isEmpty(elements)) {
                continue;
            }

            // TODO: 2018/7/21 未写完

            // 当前节点
            Element element = elements.get(0);

            // 父级 element
            Element parentElement = element.parent();

            // 所有子节点
            List<Element> children = element.children();

            // 创建新节点，将所有子节点移到新节点
            Element newElement = new Element("dev");
            children.forEach(e -> {
                newElement.appendChild(e);
            });
        }
    }
}
