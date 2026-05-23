package com.project.aminewsbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        System.out.println("[RedisService] SET key: " + key + ", value: " + value + ", timeout: " + timeout + " " + unit);
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public void set(String key, Object value) {
        System.out.println("[RedisService] SET key: " + key + ", value: " + value);
        redisTemplate.opsForValue().set(key, value);
    }

    public Object get(String key) {
        System.out.println("[RedisService] GET key: " + key);
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        System.out.println("[RedisService] DELETE key: " + key);
        redisTemplate.delete(key);
    }

    public Long getExpire(String redisKey, TimeUnit timeUnit) {
        System.out.println("[RedisService] GET EXPIRE key: " + redisKey + ", timeUnit: " + timeUnit);
        Long expire = redisTemplate.getExpire(redisKey, timeUnit);
        if (expire == null) {
            System.out.println("[RedisService] key不存在或没有设置过期时间");
            return -1L; // 返回-1表示key不存在或没有设置过期时间
        }
        if (expire == -1) {
            System.out.println("[RedisService] key没有设置过期时间");
            return -1L; // 返回-1表示key没有设置过期时间
        }
        if (expire == -2) {
            System.out.println("[RedisService] key不存在");
            return -2L; // 返回-1表示key不存在
        }
        System.out.println("[RedisService] key的剩余过期时间为: " + expire + " " + timeUnit);
        return expire;
    }
}
