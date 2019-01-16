package com.mmall.util;

import com.mmall.common.RedisPool;
import com.mmall.common.RedisShardedPool;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ShardedJedis;

@Slf4j
public class RedisShardedPoolUtill {

    public static String set(String key, String value) {
        ShardedJedis jedis = null;
        String result = null;
        try {
            jedis = RedisShardedPool.getResource();
            result = jedis.set(key,value);
        } catch (Exception e) {
            log.error("redis set, key:{},value:{}",key,value,e);
            RedisShardedPool.returnBrokenResource(jedis);
            return result;
        }
        RedisShardedPool.returnResource(jedis);
        return result;
    }

    public static Long setnx(String key, String value) {
        ShardedJedis jedis = null;
        Long result = null;
        try {
            jedis = RedisShardedPool.getResource();
            result = jedis.setnx(key,value);
        } catch (Exception e) {
            log.error("redis setnx, key:{},value:{}",key,value,e);
            RedisShardedPool.returnBrokenResource(jedis);
            return result;
        }
        RedisShardedPool.returnResource(jedis);
        return result;
    }

    public static String get(String key) {
        ShardedJedis jedis = null;
        String result = null;
        try {
            jedis = RedisShardedPool.getResource();
            result = jedis.get(key);
        } catch (Exception e) {
            log.error("redis get, key:{}",key,e);
            RedisShardedPool.returnBrokenResource(jedis);
            return  result;
        }

        RedisShardedPool.returnBrokenResource(jedis);
        return result;
    }

    // 时间exTime单位是秒
    public static String setEx(String key, int exTime, String value) {
        ShardedJedis jedis = null;
        String result = null;
        try {
            jedis = RedisShardedPool.getResource();
            result = jedis.setex(key,exTime,value);
        } catch (Exception e) {
            log.error("redis setEx, key:{},value:{}",key,value,e);
            RedisShardedPool.returnBrokenResource(jedis);
            return result;
        }
        RedisShardedPool.returnResource(jedis);
        return result;
    }

    // 设置 key 的有效期
    public static Long expire(String key, int exTime) {
        ShardedJedis jedis = null;
        Long result = null;
        try {
            jedis = RedisShardedPool.getResource();
            result = jedis.expire(key,exTime);
        } catch (Exception e) {
            log.error("redis expire, key:{},exTime:{}",key,exTime,e);
            RedisShardedPool.returnBrokenResource(jedis);
            return result;
        }
        RedisShardedPool.returnResource(jedis);
        return result;
    }

    // 删除元素
    public static Long del(String key) {
        ShardedJedis jedis = null;
        Long result = null;
        try {
            jedis = RedisShardedPool.getResource();
            result = jedis.del(key);
        } catch (Exception e) {
            log.error("redis del, key:{},value:{}",key,e);
            RedisShardedPool.returnBrokenResource(jedis);
            return result;
        }
        RedisShardedPool.returnResource(jedis);
        return result;
    }

}