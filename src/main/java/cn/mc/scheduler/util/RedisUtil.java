package cn.mc.scheduler.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * redicache 工具类
 *
 */
@Component
public class RedisUtil {

    @Resource
    public    RedisTemplate redisTemplate;
    @Autowired
    private RedisTemplate<Object, Object> template;
    /**
     * 批量删除对应的value
     *
     * @param keys
     */
    public  void remove(final String... keys) {
        for (String key : keys) {
            remove(key);
        }
    }

    /**
     * 删除对应的value
     *
     * @param key
     */
    public  void remove(final String key) {
        if (exists(key)) {
            redisTemplate.delete(key);
        }
    }
    /**
     * 判断缓存中是否有对应的value
     *
     * @param key
     * @return
     */
    public  boolean exists(final String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 读取缓存
     *
     * @param key
     * @return
     */
    public  Object get(final String key) {
        Object result = null;
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        ValueOperations<Serializable, Object> operations = redisTemplate.opsForValue();
        result = operations.get(key);
        if(result==null){
            return null;
        }
        return result;
    }
    /**
     * 写入缓存
     *
     * @param key
     * @param value
     * @return
     */
    public  boolean set(final String key, Object value) {
        boolean result = false;
        try {
            redisTemplate.opsForList().leftPush(key, value);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 写入缓存
     *
     * @param key
     * @param value
     * @return
     */
    public  boolean set(final String key, Object value, int expireTime) {
        boolean result = false;
        try {
            redisTemplate.opsForList().leftPush(key, value);
            redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public  boolean setString(final String key, Object value) {
        boolean result = false;
        try {
            ValueOperations<Serializable, Object> operations = redisTemplate.opsForValue();
            operations.set(key, value);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public  boolean hmset(String key, Map<String, String> value) {
        boolean result = false;
        try {
            redisTemplate.opsForHash().putAll(key, value);

            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public  boolean hmset(String key, Map<String, String> value, int expireTime) {
        boolean result = false;
        try {
            redisTemplate.opsForHash().putAll(key, value);
            redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public  Map<String,String> hmget(String key) {
        Map<String,String> result =null;
        try {
            result=  redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 缓存list
     * @param key
     * @param value
     * @param time
     * @return
     */
    public boolean setList(String key, Object value, long time) {
        try {
            List<Object> strings = new ArrayList<>();
            strings.add("1");
            strings.add("2");
            strings.add("3");
//            redisTemplate.opsForSet().add(key, value);
            redisTemplate.boundValueOps(key).set(value);
            if (time > 0) redisTemplate.expire(key, time, TimeUnit.SECONDS);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();;

        }
        return false;
    }

    /**
     * 获取list缓存
     * @param key
     * @return
     */
    public List<String> getList(String key) {
        long start = 0;//开始
        long end = -1;//所有
        try {
            ListOperations<String, String> listOps =  redisTemplate.opsForList();
            return listOps.range(key, start, end);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

}
