package cn.mc.scheduler;

import org.junit.Test;

/**
 * @auther sin
 * @time 2018/4/28 17:01
 */
public class CommentTest {

    @Test
    public void commentStringUtilCountMatchesTest() {
        String content = "<div><p>女生暗示喜欢你10大表现，你遇到过几个？</p><p> <p class='video-hide'>{!-- PGC_VIDEO:{\"thumb_height\": 360, \"thumb_url\": \"37ce0000f50fdedc696b\", \"vname\": \"8.24 \\u804a\\u5929 \\u4eca\\u65e5\\u5934\\u6761.mp4\", \"vid\": \"1c82f7f513454485aab77b5712f27144\", \"thumb_width\": 640, \"src_thumb_uri\": \"37ce0000f50fdedc696b\", \"sp\": \"toutiao\", \"vposter\": \"http://p0.pstatp.com/origin/37ce0000f50fdedc696b\", \"external_covers\": [{\"mimetype\": \"webp\", \"source\": \"dynpost\", \"thumb_height\": 360, \"thumb_url\": \"374b00119956d9831e81\", \"thumb_width\": 640}], \"vu\": \"1c82f7f513454485aab77b5712f27144\", \"duration\": 163, \"neardup_id\": 11779367922797408416, \"hash_id\": 1369369818420577686, \"md5\": \"522f4494f8ef05676397ec67ee740272\", \"video_size\": {\"high\": {\"h\": 480, \"subjective_score\": 0, \"w\": 854, \"file_size\": 4102405}, \"ultra\": {\"h\": 720, \"subjective_score\": 0, \"w\": 1280, \"file_size\": 8128911}, \"normal\": {\"h\": 360, \"subjective_score\": 0, \"w\": 640, \"file_size\": 3160686}}} --}</p></p></div>";
        String videoFeature = "{!-- PGC_VIDEO";
        int count = org.apache.commons.lang3.StringUtils.countMatches(content, videoFeature);
        System.out.println(count);
    }
}
