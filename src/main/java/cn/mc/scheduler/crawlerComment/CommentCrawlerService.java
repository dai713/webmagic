package cn.mc.scheduler.crawlerComment;

import cn.mc.core.dataObject.GrabUserDO;
import cn.mc.core.mybatis.Field;
import cn.mc.core.mybatis.Update;
import cn.mc.scheduler.mapper.CommentMapper;
import cn.mc.scheduler.util.AliyunOSSClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 抓取的服务类
 *
 * @author xl
 * @date 2018/8/29 下午 15:46
 */
@Component
public class CommentCrawlerService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private AliyunOSSClientUtil aliyunOSSClientUtil;

    public Long addCommentUser(GrabUserDO grabUserDO){
        try{
            //查询昵称存不存在
            List<GrabUserDO> listGrabUser=commentMapper.getGrabUser(grabUserDO.getNickName(),new Field());
            //如果没有则添加抓取的用户
            if(listGrabUser.size()<=0){
                //图像上传到我们服务器
                if(!StringUtils.isEmpty(grabUserDO.getHeadUrl())){
                    grabUserDO.setHeadUrl(aliyunOSSClientUtil.replaceSourcePicToOSS(grabUserDO.getHeadUrl()));
                }
                int code=commentMapper.insertGrabUser(Update.copyWithoutNull(grabUserDO));
                if(code==1){
                    return grabUserDO.getGrabId();
                }
            }else {
                return listGrabUser.get(0).getGrabId();
            }
        }catch (Exception ex){
            return null;
        }
        return null;
    }


}
