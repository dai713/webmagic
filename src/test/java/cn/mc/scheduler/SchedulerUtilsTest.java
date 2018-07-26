package cn.mc.scheduler;

import cn.mc.scheduler.util.SchedulerUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class SchedulerUtilsTest {


    @Autowired
    private SchedulerUtils schedulerUtils;

    @Test
    public void match() {
//        String str = "<article class=\"art_box\"> \n" +
//                " <!--标题_s-->  \n" +
//                " <!--标题_e-->   \n" +
//                " <a href=\"JavaScript:void(0)\"> \n" +
//                "  <figure class=\"art_img_mini j_p_gallery\" data-type=\"simple\"> \n" +
//                "   <img class=\"art_img_mini_img j_fullppt_cover\" src=\"https://mc-browser.oss-cn-shenzhen.aliyuncs.com/img/1016136340623851521.png\" data-src=\"//n.sinaimg.cn/sports/transform/290/w650h440/20180708/yjhG-hezpzwt3983072.jpg\" alt=\"火箭跟卡皇的续约谈判可能还要持续一阵\">  \n" +
//                "   <h2 class=\"art_img_tit\">火箭跟卡皇的续约谈判可能还要持续一阵</h2> \n" +
//                "  </figure> </a> \n" +
//                " <p class=\"art_p\"></p> \n" +
//                " <p class=\"art_p\">北京时间7月8日，据火箭记者凯利-艾科报道，火箭队受限自由球员克林特-卡培拉希望得到一份4年超过8000万美元的合同。</p> \n" +
//                " <p class=\"art_p\">到目前为止，卡培拉和火箭队的合同谈判并不顺利。很显然，卡培拉对火箭队的报价不满意，而据火箭记者凯利-艾科透露，火箭队的报价和卡培拉的心理价位非常悬殊。</p> \n" +
//                " <div sax-type=\"proxy\" class=\"j_native_ijk180709 box\" data-inserttype=\"0\" style=\"margin:20px 0\" data-id=\"201711dingdingeb\"></div>\n" +
//                " <p class=\"art_p\">“据我所知，在和火箭队的初始谈判中，卡培拉觉得自己的价值被低估。据说他希望得到一份比扎克-拉文（4年8000万美元）更大的合同，但是他得到的报价和纽基奇的差不多，也就是4年4800万美元左右。”凯利-艾科在推特上写道。</p> \n" +
//                " <p class=\"art_p\">由于火箭队的报价和卡培拉的心理价位过于悬殊，这场谈判预计会非常漫长。当然，卡培拉目前的处境比较尴尬，现在市场上并没有球队给他提供有竞争力的报价，这使得火箭队可以和他慢慢压价。</p> \n" +
//                " <p class=\"art_p\">（罗森）</p>  \n" +
//                " <script type=\"text/javascript\">window.STO=window.STO||{};window.STO.fw=new Date().getTime();</script> \n" +
//                " <!--  icon start --> \n" +
//                " <!--  icon end --> \n" +
//                "</article>";
//        schedulerUtils.keywordsReplace(str, 0, 344214423414L);
    }

    public static void main(String[] args) {

    }


}
