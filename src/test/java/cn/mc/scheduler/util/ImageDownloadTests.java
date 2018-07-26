package cn.mc.scheduler.util;

import java.io.IOException;

/**
 * @author sin
 * @time 2018/7/25 12:36
 */
public class ImageDownloadTests {

    private  static String html = "<div tabindex=\"-1\" role=\"dialog\" aria-hidden=\"false\"\n" +
            "     class=\"jsx-555271387 pswp pswp--open pswp--notouch pswp--css_animation pswp--svg pswp--animated-in pswp--visible\"\n" +
            "     style=\"z-index: 2; position: fixed; opacity: 1;\">\n" +
            "    <div class=\"jsx-555271387 pswp__bg\" style=\"opacity: 1;\"></div>\n" +
            "    <div class=\"jsx-555271387 pswp__scroll-wrap\">\n" +
            "        <div class=\"jsx-555271387 pswp__container\"\n" +
            "             style=\"transform: translate3d(0px, 0px, 0px);\">\n" +
            "            <div class=\"jsx-555271387 pswp__item\"\n" +
            "                 style=\"display: block; transform: translate3d(-394px, 0px, 0px);\"></div>\n" +
            "            <div class=\"jsx-555271387 pswp__item\"\n" +
            "                 style=\"transform: translate3d(0px, 0px, 0px);\">\n" +
            "                <div class=\"pswp__zoom-wrap\"\n" +
            "                     style=\"transform: translate3d(24px, 137px, 0px) scale(0.872);\">\n" +
            "                    <div class=\"pswp__img pswp__img--placeholder pswp__img--placeholder--blank\"\n" +
            "                         style=\"width: 666px; height: 803px; display: none;\"></div>\n" +
            "                    <img class=\"pswp__img\"\n" +
            "                         src=\"//inews.gtimg.com/newsapp_bt/0/4459140213/641?tp=webp\"\n" +
            "                         style=\"display: block; width: 375px; height: 452px;\">\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            <div class=\"jsx-555271387 pswp__item\"\n" +
            "                 style=\"display: block; transform: translate3d(394px, 0px, 0px);\">\n" +
            "                <div class=\"pswp__zoom-wrap\"\n" +
            "                     style=\"transform: translate3d(52px, 133px, 0px) scale(0.722667);\">\n" +
            "                    <div class=\"pswp__img pswp__img--placeholder pswp__img--placeholder--blank\"\n" +
            "                         style=\"width: 543px; height: 803px; display: none;\"></div>\n" +
            "                    <img class=\"pswp__img\"\n" +
            "                         src=\"//inews.gtimg.com/newsapp_bt/0/4459141386/641?tp=webp\"\n" +
            "                         style=\"width: 375px; height: 555px;\"></div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</div>\n" +
            "<div class=\"jsx-555271387 pswp__ui \">\n" +
            "    <div class=\"jsx-555271387 pswp__ui__top\" style=\"z-index: 3;\">\n" +
            "        <div class=\"jsx-555271387 topbar\"><a role=\"button\"\n" +
            "                                             data-boss=\"&amp;fun=t_return\"\n" +
            "                                             href=\"javascript:;\" target=\"_self\"\n" +
            "                                             title=\"返回\"\n" +
            "                                             class=\"jsx-555271387 back\"></a>\n" +
            "            <div class=\"jsx-555271387 top-title\"></div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    <div class=\"jsx-555271387 pswp__ui__bottom\" style=\"z-index: 3;\">\n" +
            "        <div class=\"jsx-555271387 pswp__caption\">\n" +
            "            <div class=\"jsx-555271387 pswp__caption__center\">\n" +
            "                <h1>打掉徐晓冬的牙不怕坐牢：全国拳击冠军曹恒祥与格斗狂人又起争端</h1>\n" +
            "                <span class=\"pos\">1/<em>10</em></span>\n" +
            "                自格斗狂人“武林打假”以来，引来了不少挑战者，据其统计，迄今共有34位挑战者。现在，徐晓冬方面还给这些人，一一起了外号，并如《史记列传》一般，分别著述成文，以备今后查考，且还给挑战者们各起了一个外号。不过，就是因给曾挑战过他的前全国拳击冠军曹恒祥，起了一个“地变星”的外号，二人之间风波已平后，现在又起了波澜。原来，曹恒祥在看到格斗狂人方面起的外号后，在网络上说：\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <div class=\"jsx-3933229383 \">\n" +
            "            <div class=\"nfooter-nav black comm\"><a aria-label=\"写评论\"\n" +
            "                                                   class=\"wpl icons\"\n" +
            "                                                   href=\"/c/coral/2909350280\"\n" +
            "                                                   data-boss=\"fun=f_write&amp;pagetype=tjdc\"><b>写评论</b></a><a\n" +
            "                    title=\"分享\" role=\"button\" class=\"share-btn icons\"\n" +
            "                    data-boss=\"fun=f_share&amp;pagetype=tjdc\">分享</a><a\n" +
            "                    aria-label=\"收藏\" class=\"collect icons\"\n" +
            "                    data-boss=\"fun=collect&amp;pagetype=tjdc\">收藏</a><a\n" +
            "                    aria-label=\"查看289条评论\" class=\"rpl icons\"\n" +
            "                    href=\"/c/coral/2909350280\"\n" +
            "                    data-boss=\"fun=f_read&amp;pagetype=tjdc\"><span\n" +
            "                    style=\"display: inline-block; line-height: 14px;\">289</span></a>\n" +
            "            </div>\n" +
            "            <div class=\"jsx-1837375076\">\n" +
            "                <div class=\"jsx-1837375076 share-wrap  newpic-share\">\n" +
            "                    <div class=\"jsx-1837375076 share-list\"\n" +
            "                         style=\"display: none;\">\n" +
            "                        <div class=\"jsx-3529261291\" style=\"display: block;\">\n" +
            "                            <ul data-boss=\"fun=f_share&amp;pagetype=tjdc\"\n" +
            "                                class=\"jsx-3529261291 share  undefined npic-share\">\n" +
            "                                <li data-boss=\"fun=s_wx&amp;pagetype=tjdc\"\n" +
            "                                    class=\"jsx-3529261291\"><a\n" +
            "                                        href=\"javascript:;\" target=\"_self\"\n" +
            "                                        aria-label=\"分享到微信好友\"\n" +
            "                                        class=\"jsx-3529261291 share-weixin\"><span\n" +
            "                                        class=\"jsx-3529261291\">微信</span></a>\n" +
            "                                </li>\n" +
            "                                <li data-boss=\"fun=s_pyq&amp;pagetype=tjdc\"\n" +
            "                                    class=\"jsx-3529261291\"><a\n" +
            "                                        href=\"javascript:;\" target=\"_self\"\n" +
            "                                        aria-label=\"分享到微信朋友圈\"\n" +
            "                                        class=\"jsx-3529261291 share-moments\"><span\n" +
            "                                        class=\"jsx-3529261291\">朋友圈</span></a>\n" +
            "                                </li>\n" +
            "                                <li data-boss=\"fun=s_qq&amp;pagetype=tjdc\"\n" +
            "                                    class=\"jsx-3529261291\"><a\n" +
            "                                        href=\"javascript:;\" target=\"_self\"\n" +
            "                                        aria-label=\"分享到QQ\"\n" +
            "                                        class=\"jsx-3529261291 share-qq\"><span\n" +
            "                                        class=\"jsx-3529261291\">QQ</span></a>\n" +
            "                                </li>\n" +
            "                                <li data-boss=\"fun=s_qzone&amp;pagetype=tjdc\"\n" +
            "                                    class=\"jsx-3529261291\"><a\n" +
            "                                        href=\"javascript:;\" target=\"_self\"\n" +
            "                                        aria-label=\"分享到QQ空间\"\n" +
            "                                        class=\"jsx-3529261291 share-qzone\"><span\n" +
            "                                        class=\"jsx-3529261291\">QQ空间</span></a>\n" +
            "                                </li>\n" +
            "                                <li data-boss=\"fun=s_sina&amp;pagetype=tjdc\"\n" +
            "                                    class=\"jsx-3529261291\"><a\n" +
            "                                        href=\"javascript:;\" target=\"_self\"\n" +
            "                                        aria-label=\"分享到新浪微博\"\n" +
            "                                        class=\"jsx-3529261291 share-weibo\"><span\n" +
            "                                        class=\"jsx-3529261291\">新浪微博</span></a>\n" +
            "                                </li>\n" +
            "                            </ul>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                    <div data-boss=\"fun=f_cancel&amp;pagetype=tjdc\"\n" +
            "                         class=\"jsx-1837375076 cancel\">取消\n" +
            "                    </div>\n" +
            "                    <div class=\"jsx-1837375076 border-wrap\"></div>\n" +
            "                </div>\n" +
            "                <div class=\"jsx-223325926 share-layer  \"></div>\n" +
            "                <div class=\"jsx-1837375076 layer \"></div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</div>\n";

    public static void main(String[] args) throws IOException {

        String newsContent = new AliyunOSSClientUtil().replaceSourcePicToOSS(html);

        String url = "http://inews.gtimg.com/newsapp_bt/0/4459140213/641";
//
//        Map<String, String> headers = new HashMap<>();
//        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
//        headers.put("Accept-Encoding", "gzip, deflate");
//        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
//        headers.put("Cache-Control", "no-cache");
//        headers.put("Connection", "keep-alive");
//        headers.put("Host", "inews.gtimg.com");
//        headers.put("referer", "inews.gtimg.com");
//        headers.put("Pragma", "no-cache");
//        headers.put("Upgrade-Insecure-Requests", "1");
//        headers.put("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1");
//
//        CloseableHttpResponse httpResponse = Http2Util.httpGetWithHttpResponse(url, null);
//        httpResponse.getEntity().writeTo(new FileOutputStream(new File("/Users/sin/Downloads/a.png")));
//        HttpUtil.closeQuietly(httpResponse);
    }
}
