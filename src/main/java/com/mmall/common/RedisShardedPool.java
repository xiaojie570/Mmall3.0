package com.mmall.common;

import com.google.common.collect.Lists;
import com.mmall.util.PropertiesUtil;
import redis.clients.jedis.*;
import redis.clients.util.Hashing;
import redis.clients.util.Sharded;

import java.util.List;

/**
 * Created by lenovo on 2018/12/29.
 */
public class RedisShardedPool {
    private static ShardedJedisPool pool;// ShardedJedisPool连接池
    private static Integer maxTotal = Integer.parseInt(PropertiesUtil.getProperty("redis.max.total","20"));// 最大连接数
    private static Integer maxIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.max.idle","10"));// 在 jedispool 中最大的 idle 状态（空闲状态）的jedis 实例的个数
    private static Integer minIdle = Integer.parseInt(PropertiesUtil.getProperty("redis.min.idle","2"));// 在 jedispool 中最小的 idle 状态（空闲状态）的jedis 实例的个数

    private static Boolean testOnBorrow = Boolean.valueOf(PropertiesUtil.getProperty("redis.test.borrow","true"));// 在 borrow 一个 jedis 实例的时候，是否要进行验证操作，如果是true的话，那么拿到的jedis实例一定是可用的
    private static Boolean testOnReturn = Boolean.valueOf(PropertiesUtil.getProperty("redis.test.return","true"));// 在 return 一个 jedis 实例的时候，是否要进行验证操作，如果是true的话，那么放回的jedis实例一定是可用的

    private static String redis1Ip = PropertiesUtil.getProperty("redis1.ip");
    private static Integer redis1Port = Integer.parseInt(PropertiesUtil.getProperty("redis1.port","6379"));

    private static String redis2Ip = PropertiesUtil.getProperty("redis2.ip");
    private static Integer redis2Port = Integer.parseInt(PropertiesUtil.getProperty("redis2.port","6379"));


    private static void initPool() {
        JedisPoolConfig config = new JedisPoolConfig();

        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);

        config.setTestOnBorrow(testOnBorrow);
        config.setTestOnReturn(testOnReturn);

        config.setBlockWhenExhausted(true); // 连接耗尽的时候，是否阻塞，false 会抛出异常，true 阻塞直到超时。默认为true

        JedisShardInfo info1 = new JedisShardInfo(redis1Ip,redis1Port,100);
        JedisShardInfo info2 = new JedisShardInfo(redis2Ip,redis2Port,100);

        List<JedisShardInfo> jedisShardInfoList = Lists.newArrayList();
        jedisShardInfoList.add(info1);
        jedisShardInfoList.add(info2);


        pool = new ShardedJedisPool(config,jedisShardInfoList, Hashing.MURMUR_HASH, Sharded.DEFAULT_KEY_TAG_PATTERN); // 2 秒
    }

    static {
        initPool();
    }

    public static ShardedJedis getResource() {
        return pool.getResource();
    }

    public static void returnResource(ShardedJedis jedis) {
        pool.returnResource(jedis);
    }

    public static void returnBrokenResource(ShardedJedis jedis) {
        pool.returnBrokenResource(jedis);
    }

    public static void main(String[] args) {
        ShardedJedis jedis = pool.getResource();

        for(int i =0 ; i < 10; i++) {
            jedis.set("key:" + i, "value:" + i);
        }

        pool.returnResource(jedis);
        System.out.println("end....");
    }
}
