package cn.mc.scheduler.crawler;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.zip.CRC32;

/**
 * @auther sin
 * @time 2018/3/19 15:39
 */
public class VideoTest {



    public static void main(String[] args) throws UnsupportedEncodingException {
        String videoId = "f1355b54a0b442248fae08fae935348d";
        // NSString *urlString=[NSString stringWithFormat:@"/video/urls/v/1/toutiao/mp4/%@?r=%ld",videoID,srandom];
        int random = (int) ((int)1000000000 + (Math.random() * 1000000000));
        String a = String.format("/video/urls/v/1/toutiao/mp4/%s?r=%s", videoId, random);

        CRC32 crc32 = new CRC32();
        crc32.update(a.getBytes());
        Long crc32Value = crc32.getValue();


//        NSString *getUrlStr=[NSString stringWithFormat:@"http://i.snssdk.com/video/urls/v/1/toutiao/mp4/%@?r=%ld&s=%ld&callback=",videoID,srandom,crcint];
        String videoUrl = String.format("http://i.snssdk.com/video/urls/v/1/toutiao/mp4/%s?r=%s&s=%s&callback=", videoId, random, crc32Value);

        System.out.println(videoUrl);
//        System.out.println(videoUrl);
//        System.out.println(new String((Base64.getDecoder().decode(videoUrl)), "UTF-8"));

//        String base64 = "aHR0cDovL3Y3LnBzdGF0cC5jb20vYzZkMzRjMTMzYWY3ZWY0OWVjNTViM2VhZGU4MDU3NWQvNWFhYTM3YTIvdmlkZW8vbS8yMjBhNzc2ZWYyNjkzYmI0MTQxOTFlOGQzMTg5NTUwMWIxYTExNTVhOTExMDAwMDY2ZTc1Njg3NTJhNS8";
        String base64 = "aHR0cDovL3YxLXR0Lml4aWd1YXZpZGVvLmNvbS8yZmFhMjRkMjA1Mzc1ODE4MDdiOGZmZmFkMTMzZmFiNC81YWIzYTUwZC92aWRlby9tLzIyMDFlNTEzNGQ3MDUxMDQyZDY5MWRhOGEwNjRiMDhlYTU4MTE1NTYzNDAwMDAwYWI1N2NkM2ZhZmUyLw==";
        System.out.println(new String(Base64.getDecoder().decode(base64), "UTF-8"));
    }
}
