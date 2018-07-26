package cn.mc.scheduler.crawler.toutiao.com;

import cn.mc.scheduler.SchedulerApplication;
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SchedulerApplication.class)
public class ToutiaoIndexInfoTest {

    @Autowired
    private ToutiaoIndexInfoCrawler toutiaoIndexInfo;

    @Autowired
    private ToutiaoPictureCrawler toutiaoPictureCrawler;

    @Autowired
    private ToutiaoFunnyCrawler toutiaoFunnyCrawler;

    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;

    /**
     * 今日头条-推荐栏目新闻爬取数据测试
     */
    @Test
    public void indexInfoTest(){
        toutiaoIndexInfo.createCrawler().thread(3).run();
    }

    /**
     * 今日头条-图片信息爬取数据测试
     */
    @Test
    public void pictureTest(){
        toutiaoPictureCrawler.createCrawler().thread(3).run();
    }

    /**
     * 今日头条-搞笑新闻爬取数据测试
     */
    @Test
    public void funnyTest(){
        toutiaoFunnyCrawler.createCrawler().thread(3).run();
    }

    @Test
    public void upload() {
        String url = "http://img05.tooopen.com/images/20150820/tooopen_sy_139205349641.jpg";
        String result = aliyunOSSClientUtil.uploadPicture(url);
        System.out.println("上传后返回url：" + result);
    }

    public static void main(String[] args) {
        String str = "问答";
        String str2 = "悟空问答";
        if (str2.contains(str)) {
            System.out.println("match success");
        } else {
            System.out.println("match fails");
        }
    }

}
