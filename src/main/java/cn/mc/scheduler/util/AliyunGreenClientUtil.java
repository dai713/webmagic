package cn.mc.scheduler.util;

import cn.mc.scheduler.SchedulerProperties;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.green.model.v20170112.ImageSyncScanRequest;
import com.aliyuncs.green.model.v20170112.TextScanRequest;
import com.aliyuncs.green.model.v20170112.VideoAsyncScanRequest;
import com.aliyuncs.green.model.v20170112.VideoAsyncScanResultsRequest;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AliyunGreenClientUtil {
    @Autowired
    private SchedulerProperties schedulerProperties;
    //获取配置的正则表达式
    private static  String REGXPFORTAG="<\\s*img\\s+([^>]*)\\s*";
    //获取配置的正则表达式
    private static  String REGXPFORTAGATTRIB="src=\\s*\"([^\"]+)\"";
    public JSONObject getVideoAsyncScanResults(String taskId){
        try {
            //替换accessKeyId、accessKeySecret
            IClientProfile profile = DefaultProfile.getProfile(schedulerProperties.securityRegionId, schedulerProperties.securityAccessKeyId, schedulerProperties.securityAccessKeySecret);
            DefaultProfile.addEndpoint(schedulerProperties.securityRegionId, schedulerProperties.securityRegionId, "Green", getDomain(schedulerProperties.securityRegionId)); // 添加自定义endpoint。

            IAcsClient client = new DefaultAcsClient(profile);

            VideoAsyncScanResultsRequest videoAsyncScanResultsRequest = new VideoAsyncScanResultsRequest();
            videoAsyncScanResultsRequest.setAcceptFormat(FormatType.JSON); // 指定api返回格式


            JSONArray data = new JSONArray();
            data.add(taskId);

            videoAsyncScanResultsRequest.setContent(data.toJSONString().getBytes("UTF-8"), "UTF-8", FormatType.JSON);

            /**
             * 请务必设置超时时间
             */
            videoAsyncScanResultsRequest.setConnectTimeout(10000);
            videoAsyncScanResultsRequest.setReadTimeout(10000);
            HttpResponse httpResponse = client.doAction(videoAsyncScanResultsRequest);

            if(httpResponse.isSuccess()){
                JSONObject jo = JSON.parseObject(new String(httpResponse.getContent(), "UTF-8"));
                System.out.println(JSON.toJSONString(jo, true));
                return  jo;

            }else{
                JSONObject jsonObject=new JSONObject();
                jsonObject.put("code",httpResponse.getStatus());
                System.out.println("response not success. status:" + httpResponse.getStatus());
                return  jsonObject;
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return null;
    }
    //提交视频校验
    public JSONObject submitVideoAsyncScanResults(Long newsId,String url,Integer videoLength) {
        try{
            //替换accessKeyId、accessKeySecret
            IClientProfile profile = DefaultProfile.getProfile(schedulerProperties.securityRegionId, schedulerProperties.securityAccessKeyId, schedulerProperties.securityAccessKeySecret);
            DefaultProfile.addEndpoint(schedulerProperties.securityRegionId, schedulerProperties.securityRegionId, "Green", getDomain(schedulerProperties.securityRegionId));
            IAcsClient client = new DefaultAcsClient(profile);

            VideoAsyncScanRequest videoAsyncScanRequest = new VideoAsyncScanRequest();
            videoAsyncScanRequest.setAcceptFormat(FormatType.JSON); // 指定api返回格式
            videoAsyncScanRequest.setMethod(com.aliyuncs.http.MethodType.POST); // 指定请求方法

            List<Map<String, Object>> tasks = new ArrayList<Map<String, Object>>();
            Map<String, Object> task = new LinkedHashMap<String, Object>();
            task.put("dataId", newsId);
            task.put("interval", 1);
            task.put("url", url);
            tasks.add(task);

            JSONObject data = new JSONObject();
            data.put("scenes", Arrays.asList("porn","terrorism"));
            data.put("tasks", tasks);

            videoAsyncScanRequest.setContent(data.toJSONString().getBytes("UTF-8"), "UTF-8", FormatType.JSON);

            /**
             * 请务必设置超时时间
             */
            videoAsyncScanRequest.setConnectTimeout(10000);
            videoAsyncScanRequest.setReadTimeout(10000);
            HttpResponse httpResponse = client.doAction(videoAsyncScanRequest);

            if(httpResponse.isSuccess()){
                JSONObject jo = JSON.parseObject(new String(httpResponse.getContent(), "UTF-8"));
                System.out.println(JSON.toJSONString(jo, true));
                if(jo.getInteger("code")==200){
                    JSONArray jsonArray= (JSONArray) jo.get("data");
                    System.out.println(jsonArray.size());
                    if(jsonArray.size()>0){
                        JSONObject jsonObject= (JSONObject) jsonArray.get(0);
                        return jsonObject;
                    }else {
                        return null;
                    }
                }
            }else{
                System.out.println("response not success. status:" + httpResponse.getStatus());
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return  null;
    }
    public JSONObject imageSyncScanRequest(String imgUrl) throws Exception{
        //请替换成你自己的accessKeyId、accessKeySecret
        IClientProfile profile = DefaultProfile.getProfile(schedulerProperties.securityRegionId, schedulerProperties.securityAccessKeyId, schedulerProperties.securityAccessKeySecret);
        DefaultProfile.addEndpoint(schedulerProperties.securityRegionId, schedulerProperties.securityRegionId, "Green", getDomain(schedulerProperties.securityRegionId));
        IAcsClient client = new DefaultAcsClient(profile);

        ImageSyncScanRequest imageSyncScanRequest = new ImageSyncScanRequest();
        imageSyncScanRequest.setAcceptFormat(FormatType.JSON); // 指定api返回格式
        imageSyncScanRequest.setMethod(MethodType.POST); // 指定请求方法
        imageSyncScanRequest.setEncoding("utf-8");
        imageSyncScanRequest.setRegionId(schedulerProperties.securityRegionId);

        List<Map<String, Object>> tasks = new ArrayList<Map<String, Object>>();
        Map<String, Object> task = new LinkedHashMap<String, Object>();
        task.put("dataId", UUID.randomUUID().toString());
        task.put("url", imgUrl);
        task.put("time", new Date());

        tasks.add(task);
        JSONObject data = new JSONObject();
        /**
         * porn: 色情
         * terrorism: 暴恐
         * qrcode: 二维码
         * ad: 图片广告
         * ocr: 文字识别
         */
        data.put("scenes", Arrays.asList("porn", "terrorism", "ocr"));//添加色情和暴恐检测
        data.put("tasks", tasks);

        imageSyncScanRequest.setContent(data.toJSONString().getBytes("UTF-8"), "UTF-8", FormatType.JSON);
        /**
         * 设置超时时间
         */
        imageSyncScanRequest.setConnectTimeout(20000);
        imageSyncScanRequest.setReadTimeout(20000);
        JSONObject result = new JSONObject();
        try {
            HttpResponse httpResponse = client.doAction(imageSyncScanRequest);

            if (httpResponse.isSuccess()) {
                JSONObject scrResponse = JSON.parseObject(new String(httpResponse.getContent(), "UTF-8"));
                if (200 == scrResponse.getInteger("code")) {
                    JSONArray taskResults = scrResponse.getJSONArray("data");
                    for (Object taskResult : taskResults) {
                        if (200 == ((JSONObject) taskResult).getInteger("code")) {
                            JSONArray sceneResults = ((JSONObject) taskResult).getJSONArray("results");
                            result.put("code",200);
                            for (Object sceneResult : sceneResults) {
                                String scene = ((JSONObject) sceneResult).getString("scene");
                                String suggestion = ((JSONObject) sceneResult).getString("suggestion");
                                System.out.println("args = [" + scene + "]");
                                System.out.println("args = [" + suggestion + "]");
                                result.put("scene",scene);
                                result.put("suggestion",suggestion);
                                //根据scene和suggetion做相关的处理
                                //如果是色情的审核结果
                                if(scene.equals("porn")){
                                   if(!suggestion.equals("pass")){
                                       result.put("scuess",false);

                                       return result;
                                   }
                                }
                                //如果是暴恐的审核结果
                                if(scene.equals("terrorism")){
                                    if(!suggestion.equals("pass")){
                                        result.put("scuess",false);
                                        return result;
                                    }
                                }
                                //如果是图片广告的审核结果
                                if(scene.equals("ad")){
                                    if(!suggestion.equals("pass")){
                                        result.put("scuess",false);
                                        return result;
                                    }
                                }
                                result.put("scuess",true);
                                return result;
                            }
                        } else {
                            result.put("code",((JSONObject) taskResult).getInteger("code"));
                            result.put("scuess",false);
                            System.out.println("task process fail:" + ((JSONObject) taskResult).getInteger("code"));
                        }
                    }
                } else {
                    result.put("code",scrResponse.getInteger("code"));
                    result.put("scuess",false);
                    System.out.println("detect not success. code:" + scrResponse.getInteger("code"));
                }
            } else {
                result.put("code",httpResponse.getStatus());
                result.put("scuess",false);
                System.out.println("response not success. status:" + httpResponse.getStatus());
            }
        } catch (Exception e) {
            result.put("code","500");
            result.put("scuess",false);
            e.printStackTrace();
        }
        return  result;
    }
    public  JSONObject textFilter(String comment) {
        JSONObject result = new JSONObject();
        try {

            IClientProfile profile = DefaultProfile.getProfile(schedulerProperties.securityRegionId, schedulerProperties.securityAccessKeyId, schedulerProperties.securityAccessKeySecret);
            DefaultProfile.addEndpoint(schedulerProperties.securityRegionId, schedulerProperties.securityRegionId, "Green", getDomain(schedulerProperties.securityRegionId));

            IAcsClient client = new DefaultAcsClient(profile);

            TextScanRequest textScanRequest = new TextScanRequest();
            textScanRequest.setAcceptFormat(FormatType.JSON); // 指定api返回格式
            textScanRequest.setContentType(FormatType.JSON);
            textScanRequest.setMethod(com.aliyuncs.http.MethodType.POST); // 指定请求方法
            textScanRequest.setEncoding("UTF-8");
            textScanRequest.setRegionId(schedulerProperties.securityRegionId);

            List<Map<String, Object>> tasks = new ArrayList<Map<String, Object>>();

            Map<String, Object> task1 = new LinkedHashMap<String, Object>();
            task1.put("dataId", UUID.randomUUID().toString());
            task1.put("content", comment);

            tasks.add(task1);

            JSONObject data = new JSONObject();
            data.put("scenes", Arrays.asList("antispam"));
            data.put("tasks", tasks);
            textScanRequest.setContent(data.toJSONString().getBytes("UTF-8"), "UTF-8", FormatType.JSON);

            /**
             * 请务必设置超时时间
             */
            textScanRequest.setConnectTimeout(3000);
            textScanRequest.setReadTimeout(6000);

            HttpResponse httpResponse = client.doAction(textScanRequest);

            if(httpResponse.isSuccess()){
                JSONObject scrResponse = JSON.parseObject(new String(httpResponse.getContent(), "UTF-8"));
                System.out.println(JSON.toJSONString(scrResponse, true));
                if (200 == scrResponse.getInteger("code")) {
                    JSONArray taskResults = scrResponse.getJSONArray("data");
                    for (Object taskResult : taskResults) {
                        if(200 == ((JSONObject)taskResult).getInteger("code")){
                            result.put("code",200);
                            JSONArray sceneResults = ((JSONObject)taskResult).getJSONArray("results");
                            for (Object sceneResult : sceneResults) {
                                String scene = ((JSONObject)sceneResult).getString("scene");
                                String suggestion = ((JSONObject)sceneResult).getString("suggestion");
                                result.put("scene",scene);
                                result.put("suggestion",suggestion);
                                if(!suggestion.equals("pass")){
                                    result.put("scuess",false);
                                    result.put("object",((JSONObject)sceneResult).getString("details"));
                                    return result;
                                }
                                result.put("scuess",true);
                                result.put("object",((JSONObject)sceneResult).getString("details"));
                                return result;
                            }
                        }else{
                            result.put("code",((JSONObject)taskResult).getInteger("code"));
                            result.put("scuess",false);
                            System.out.println("task process fail:" + ((JSONObject)taskResult).getInteger("code"));
                        }
                    }
                } else {
                    result.put("code",scrResponse.getInteger("code"));
                    result.put("scuess",false);
                    System.out.println("detect not success. code:" + scrResponse.getInteger("code"));
                }
            }else{
                result.put("code",httpResponse.getStatus());
                result.put("scuess",false);
                System.out.println("response not success. status:" + httpResponse.getStatus());
            }
        } catch (Exception e) {
            result.put("code","500");
            result.put("scuess",false);
            e.printStackTrace();
        }
        return result;
    }
    //校验图片的地址
    public  JSONObject  reviewPicToAliGreen(String content){
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
                        return  this.imageSyncScanRequest(imgFilterUrl);

                    }else { //没有http则默认添加
                        String url="https:"+imgFilterUrl;
                        return this.imageSyncScanRequest(url);
                    }
                }
                matcherForAttrib.appendTail(sbreplace);
                matcherForTag.appendReplacement(sb, sbreplace.toString());
                result = matcherForTag.find();
            }
            //如果找不到img则是直接的图片url
            if(result==false){
                String imgFilterUrl=new String(content.replaceAll("amp;","").trim());
                if (imgFilterUrl.startsWith("http:") || imgFilterUrl.startsWith("https:")) {
                    return this.imageSyncScanRequest(imgFilterUrl);
                }else{
                    JSONObject jsonObject=new JSONObject();
                    jsonObject.put("scuess",true);
                    jsonObject.put("code",200);//没有图片校验的
                    return jsonObject;
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
            JSONObject jsonObject=new JSONObject();
            jsonObject.put("scuess",false);
            jsonObject.put("code",500);//文章解析失败
            return jsonObject;
        }
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("scuess",false);
        jsonObject.put("code",505);//文章解析失败
        return jsonObject;
    }
    protected static String getDomain(String regionId){
        if("cn-shanghai".equals(regionId)){
            return "green.cn-shanghai.aliyuncs.com";
        }

        if("cn-hangzhou".equals(regionId)){
            return "green.cn-hangzhou.aliyuncs.com";
        }

        if("local".equals(regionId)){
            return "api.green.alibaba.com";
        }

        return "green.cn-shanghai.aliyuncs.com";
    }
}
