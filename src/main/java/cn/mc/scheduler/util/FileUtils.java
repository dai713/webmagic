package cn.mc.scheduler.util;

import cn.mc.core.utils.AliyunOSSUtil;
import cn.mc.core.utils.BeanManager;
import cn.mc.scheduler.SchedulerProperties;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public class FileUtils {

    private static SchedulerProperties schedulerProperties = null;
    static {
        schedulerProperties = BeanManager.getBean(SchedulerProperties.class);
    }

    /**
     * 上传文件到阿里云并返回url
     * @param inputStream
     * @return
     */
    public static String uploadFile(String uploadFileName, InputStream inputStream){
        String fileName = "";
        try {
            fileName = AliyunOSSUtil.uploadFile(uploadFileName, inputStream,schedulerProperties.endPoint,schedulerProperties.accessKeyId,schedulerProperties.accessKeySecret,schedulerProperties.buildUrl,schedulerProperties.bucketName);
        } catch (Exception e) {
            e.printStackTrace();
            return fileName;
        }
        return fileName;
    }

    /**
     * 上传文件到阿里云并返回url
     * @param multipartFile
     * @return
     */
    public static String uploadFile(MultipartFile multipartFile){
        String fileName = "";
        try {
            String uploadFileName = multipartFile.getOriginalFilename();
            uploadFileName = System.currentTimeMillis() + uploadFileName.substring(uploadFileName.lastIndexOf("."));
            fileName = AliyunOSSUtil.uploadFile(uploadFileName, multipartFile.getInputStream(),schedulerProperties.endPoint,schedulerProperties.accessKeyId,schedulerProperties.accessKeySecret,schedulerProperties.buildUrl,schedulerProperties.bucketName);
        } catch (Exception e) {
            e.printStackTrace();
            return fileName;
        }
        return fileName;
    }

    /**
     * 根据url删除阿里云文件
     * @param url
     * @return
     */
    public static int deleteFile(String url) {
        int code = 0;
        try {
            String urlKey = url.substring(url.indexOf("img"));
            AliyunOSSUtil.deleteFile(schedulerProperties.endPoint,schedulerProperties.accessKeyId,schedulerProperties.accessKeySecret,schedulerProperties.bucketName, urlKey);
        } catch (Exception e) {
            e.printStackTrace();
            code = -1;
        }
        return code;
    }

}
