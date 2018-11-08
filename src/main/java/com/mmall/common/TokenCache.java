package com.mmall.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by lenovo on 2018/10/8.
 */
public class TokenCache {
    // 声明一下这个类的日志文件
    private static Logger logger = LoggerFactory.getLogger(TokenCache.class);


    public static final String TOKEN_PREFIX = "token_";
    // LRU算法：当大于设置的最大缓存项的时候，就使用LRU算法来进行清除
    // 声明一个静态的内存块， 构建本地Cache . 初始化（设置缓存的初始化容量1000）.设置缓存的最大容量（10000）.设置有效期（12小时）.
    private static LoadingCache<String,String> loadCache = CacheBuilder.newBuilder().initialCapacity(1000).maximumSize(10000).expireAfterAccess(12, TimeUnit.HOURS)
            .build(new CacheLoader<String, String>() {
                // 默认的数据加载实现，当调用get取值的时候，如果key没有对应的值，就调用这个方法进行加载
                @Override
                public String load(String s) throws Exception {
                    return "null";
                }
            });

    public static void setKey(String key, String value) {
        loadCache.put(key,value);
    }

    public static String getKey(String key) {
        String value = null;
        try {
            value = loadCache.get(key);
            if("null".equals(value)){
                return null;
            }
            return value;
        }catch (Exception e) {
            logger.error("localCache get error",e);
        }
        return null;
    }
}
