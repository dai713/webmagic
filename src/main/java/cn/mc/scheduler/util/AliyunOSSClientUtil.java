package cn.mc.scheduler.util;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.mc.core.utils.Http2Util;
import cn.mc.core.utils.HttpUtil;
import cn.mc.core.utils.IDUtil;
import cn.mc.scheduler.SchedulerProperties;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.log4j.Logger;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.Bucket;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;

/**
 * @class:AliyunOSSClientUtil
 * @author xl
 */

// TODO: 2018/4/21 重写
@Component
public class AliyunOSSClientUtil {

    @Autowired
    private  SchedulerProperties schedulerProperties;
    //log日志
    private static Logger logger = Logger.getLogger(AliyunOSSClientUtil.class);
    //阿里云API的文件夹名称
    private static String FOLDER="img";
    //获取配置的正则表达式
    private static  String REGXPFORTAG="<\\s*img\\s+([^>]*)\\s*";
    //获取配置的正则表达式
    private static  String REGXPFORTAGATTRIB="src=\\s*\"([^\"]+)\"";
    /**
     * 获取阿里云OSS客户端对象
     * @return ossClient
     */
    public  OSSClient getOSSClient(){
        return new OSSClient(schedulerProperties.endPoint,schedulerProperties.accessKeyId, schedulerProperties.accessKeySecret);
    }

    /**
     * 创建存储空间
     * @param ossClient      OSS连接
     * @param bucketName 存储空间
     * @return
     */
    public  static String createBucketName(OSSClient ossClient,String bucketName){
        //存储空间
        final String bucketNames=bucketName;
        if(!ossClient.doesBucketExist(bucketName)){
            //创建存储空间
            Bucket bucket=ossClient.createBucket(bucketName);
            logger.info("创建存储空间成功");
            return bucket.getName();
        }
        return bucketNames;
    }

    /**
     * 删除存储空间buckName
     * @param ossClient  oss对象
     * @param bucketName  存储空间
     */
    public static  void deleteBucket(OSSClient ossClient, String bucketName){
        ossClient.deleteBucket(bucketName);
        logger.info("删除" + bucketName + "Bucket成功");
    }

    /**
     * 创建模拟文件夹
     * @param ossClient oss连接
     * @param bucketName 存储空间
     * @param folder   模拟文件夹名
     * @return  文件夹名
     */
    public  static String createFolder(OSSClient ossClient,String bucketName,String folder){
        //文件夹名
        final String keySuffixWithSlash =folder;
        //判断文件夹是否存在，不存在则创建
        if(!ossClient.doesObjectExist(bucketName, keySuffixWithSlash)){
            //创建文件夹
            ossClient.putObject(bucketName, keySuffixWithSlash, new ByteArrayInputStream(new byte[0]));
            logger.info("创建文件夹成功");
            //得到文件夹名
            OSSObject object = ossClient.getObject(bucketName, keySuffixWithSlash);
            String fileDir=object.getKey();
            return fileDir;
        }
        return keySuffixWithSlash;
    }


    /**
     * 上传图片至OSS
     * @param ossClient  oss连接
     * @param bucketName  存储空间
     * @return String 返回的唯一MD5数字签名
     * */
    public  String uploadObject2OSS(OSSClient ossClient, InputStream is, String bucketName, String folder,String fileName) {
        String resultStr = null;
        try {
            //上传文件   (上传文件流的形式)
            PutObjectResult putResult = ossClient.putObject(bucketName, folder +"/"+ fileName, is);
            //解析结果
            resultStr = putResult.getETag();
            ossClient.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("上传阿里云OSS服务器异常." + e.getMessage(), e);
        }
        return resultStr;
    }
    //上传图片到阿里云服务器
    public String uploadPicture(String url){
        String fileName;
        if(url.contains(".gif")){
            fileName = String.valueOf(IDUtil.getNewID())+".gif";
        }else{
            fileName = String.valueOf(IDUtil.getNewID())+".png";
        }
        OSSClient ossClient = getOSSClient();
        try {
            InputStream inputStream=ImgUtil.getInputStream(url);
            this.uploadObject2OSS(ossClient, inputStream,
                    schedulerProperties.bucketName, FOLDER,fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  schedulerProperties.buildUrl+"/"+FOLDER+"/"+fileName;
    }

    //替换字符串并且
    public  String  replaceSourcePicToOSS(String content){
        try{
            Pattern patternForTag = Pattern.compile (REGXPFORTAG,Pattern. CASE_INSENSITIVE );
            Pattern patternForAttrib = Pattern.compile (REGXPFORTAGATTRIB,Pattern. CASE_INSENSITIVE );
            Matcher matcherForTag = patternForTag.matcher(content);
            StringBuffer sb = new StringBuffer();
            boolean result = matcherForTag.find();
            while (result) {

                StringBuffer sbreplace = new StringBuffer( "<img ");
                Matcher matcherForAttrib = patternForAttrib.matcher(matcherForTag.group(1));
                if (matcherForAttrib.find()) {
                    String attributeStr = matcherForAttrib.group(1);
                    String imgFilterUrl=new String(attributeStr.replaceAll("amp;","").trim());
                    if (imgFilterUrl.startsWith("http:") || imgFilterUrl.startsWith("https:")) {
                        //查询图片是否是二维码
                        Boolean isQRcode=checkImgQRcode(imgFilterUrl);
                        if(isQRcode){
                            matcherForAttrib.appendReplacement(sbreplace, "");
                        }else{
                            // 上传文件
                            String picUrl=this.uploadPicture(imgFilterUrl);
                            matcherForAttrib.appendReplacement(sbreplace, "src=\""+picUrl+"\"");
                            matcherForAttrib.appendTail(sbreplace);
                        }

                    }else { //没有http则默认添加
                        String url="http:"+imgFilterUrl;
                        //查询图片是否是二维码
                        Boolean isQRcode=checkImgQRcode(imgFilterUrl);
                        if(isQRcode){
                            matcherForAttrib.appendReplacement(sbreplace, "");
                        }else{
                            String picUrl=this.uploadPicture(url);
                            matcherForAttrib.appendReplacement(sbreplace, "src=\""+picUrl+"\"");
                            matcherForAttrib.appendTail(sbreplace);
                        }

                    }
                }

                matcherForTag.appendReplacement(sb, sbreplace.toString());
                result = matcherForTag.find();
            }
            //如果找不到img则是直接的图片url
            if(result==false){
                String imgFilterUrl=new String(content.replaceAll("amp;","").trim());
                if (imgFilterUrl.startsWith("http:") || imgFilterUrl.startsWith("https:")) {
                    // 下载文件
                    String picUrl=this.uploadPicture(imgFilterUrl);
                    return picUrl;
                }
            }
            matcherForTag.appendTail(sb);
            //去掉无用的img标签
            content=sb.toString();
            content=content.replaceAll("<img >","");
            return content;
        }catch (Exception ex){
            logger.error(ex.getMessage());
            return content;
        }
    }
    private  boolean checkImgQRcode(String imgUrl){
        BufferedImage bufferedImage=null;
        try {
            MultiFormatReader formatReader = new MultiFormatReader();
            InputStream inputStream=ImgUtil.getInputStream(imgUrl);
            bufferedImage = ImageIO.read(inputStream);
            BinaryBitmap binaryBitmap= new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(bufferedImage)));
            //定义二维码参数
            Map hints= new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            Result result = formatReader.decode(binaryBitmap, hints);
            //输出相关的二维码信息
            System.out.println("解析结果"+result.toString());
            System.out.println("二维码格式类型"+result.getBarcodeFormat());
            System.out.println("二维码文本内容"+result.getText());
            bufferedImage.flush();
        } catch (NotFoundException e) {
            if(null!=bufferedImage){
                bufferedImage.flush();
            }
            return  false;
        } catch (IOException e) {
            if(null!=bufferedImage){
                bufferedImage.flush();
            }
            return false;
        }
        return true;
    }
    //测试
//    public static void main(String[] args) throws Exception {
//        <img src="http://p3.pstatp.com/large/6ecc0008d1ba7e5920db" img_width="500" img_height="449" alt="经纪人胖回出厂设置？孙俪怒怼：她在母乳期！" inline="0"><p>近日孙俪在出席活动时意外被后面的经纪人郭思抢镜了，原因是，郭思之前减肥成功逆袭成女神后，如今又胖回去了、回到出厂设置、腿上肉圆滚滚！</p><img src="http://p3.pstatp.com/large/6ec900117c723defff00" img_width="500" img_height="408" alt="经纪人胖回出厂设置？孙俪怒怼：她在母乳期！" inline="0"><p>孙俪在微博解释思思再度胖起来的原因：因为思思还在母乳期！</p><p>孙俪还表示：有我在胖是不可能的！</p><img src="https://mc-browser.oss-cn-shenzhen.aliyuncs.com/img/987254988919013376.png" img_width="500" img_height="221" alt="经纪人胖回出厂设置？孙俪怒怼：她在母乳期！" inline="0"><p>不熟悉孙俪的人会问，这个郭思思是谁？其实她以前是孙俪的助理，现在是孙俪和邓超的经纪人。</p><img src="https://mc-browser.oss-cn-shenzhen.aliyuncs.com/img/987255061656633344.png" img_width="500" img_height="406" alt="经纪人胖回出厂设置？孙俪怒怼：她在母乳期！" inline="0"><p>邓超曾说，郭思比老婆跟自己生活的时间还要长，孙俪表示：“今年是我们工作的第八个年头，但从来没有经历过七年之痒，还挺好的。思思外面的坏形象都是为了保护我，体现我更美好的形象。感谢你！”</p><img src="https://mc-browser.oss-cn-shenzhen.aliyuncs.com/img/987255112462237696.png" img_width="500" img_height="422" alt="经纪人胖回出厂设置？孙俪怒怼：她在母乳期！" inline="0"><p>孙俪还表示，当时拍完《甄嬛传》，郭思对自己说过最动情的话是“愿意做你一辈子的槿汐姑姑”。邓超和孙俪早已经把郭思当成家人了。</p><img src="https://mc-browser.oss-cn-shenzhen.aliyuncs.com/img/987255138122989568.png" img_width="500" img_height="393" alt="经纪人胖回出厂设置？孙俪怒怼：她在母乳期！" inline="0"><p>去年孙俪还专门发微博，晒出郭思的减肥前后对比图，成功减掉65斤，震惊无数吃瓜群众。</p>
//        String url="<img src=\"http://p3.pstatp.com/large/6ecc0008d1ba7e5920db\" img_width=\"500\" img_height=\"449\" alt=\"经纪人胖回出厂设置？孙俪怒怼：她在母乳期！\" inline=\"0\"><p>近日孙俪在出席活动时意外被后面的经纪人郭思抢镜了，原因是，郭思之前减肥成功逆袭成女神后，如今又胖回去了、回到出厂设置、腿上肉圆滚滚！</p><img src=\"http://p3.pstatp.com/large/6ec900117c723defff00\" img_width=\"500\" img_height=\"408\" alt=\"经纪人胖回出厂设置？孙俪怒怼：她在母乳期！\" inline=\"0\"><p>孙俪在微博解释思思再度胖起来的原因：因为思思还在母乳期！</p><p>孙俪还表示：有我在胖是不可能的！</p><img src=\"http://p1.pstatp.com/large/6ec4000a26b308dd0e00\" img_width=\"500\" img_height=\"221\" alt=\"经纪人胖回出厂设置？孙俪怒怼：她在母乳期！\" inline=\"0\"><p>不熟悉孙俪的人会问，这个郭思思是谁？其实她以前是孙俪的助理，现在是孙俪和邓超的经纪人。</p><img src=\"http://p3.pstatp.com/large/6ec4000a26b4e337e33a\" img_width=\"500\" img_height=\"406\" alt=\"经纪人胖回出厂设置？孙俪怒怼：她在母乳期！\" inline=\"0\"><p>邓超曾说，郭思比老婆跟自己生活的时间还要长，孙俪表示：“今年是我们工作的第八个年头，但从来没有经历过七年之痒，还挺好的。思思外面的坏形象都是为了保护我，体现我更美好的形象。感谢你！”</p><img src=\"http://p1.pstatp.com/large/6ec70011af022fbee840\" img_width=\"500\" img_height=\"422\" alt=\"经纪人胖回出厂设置？孙俪怒怼：她在母乳期！\" inline=\"0\"><p>孙俪还表示，当时拍完《甄嬛传》，郭思对自己说过最动情的话是“愿意做你一辈子的槿汐姑姑”。邓超和孙俪早已经把郭思当成家人了。</p><img src=\"http://p3.pstatp.com/large/6eca000a375fdff476ee\" img_width=\"500\" img_height=\"393\" alt=\"经纪人胖回出厂设置？孙俪怒怼：她在母乳期！\" inline=\"0\"><p>去年孙俪还专门发微博，晒出郭思的减肥前后对比图，成功减掉65斤，震惊无数吃瓜群众。</p>";
//          System.out.println(AliyunOSSClientUtil.replaceSourcePicToOSS(url));
//          <img src="http://p1.pstatp.com/large/6ec4000a26b308dd0e00" img_width="500" img_height="221" alt="经纪人胖回出厂设置？孙俪怒怼：她在母乳期！" inline="0">
//        System.out.println(uploadPicture(urlFliter));
//    }


}