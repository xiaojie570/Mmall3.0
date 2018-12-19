package com.mmall.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Created by lenovo on 2018/10/9.
 */

/**
 * 读取配置文件类
 */
public class PropertiesUtil {
    // 日志文件
    private static Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);

    // 使用的是util包下的Properties
    private static Properties props;

    // 因为在tomcat启动的时候就需要加载这个配置，所以使用静态代码块。静态代码块在类被加载的时候被执行，且只执行一次，一般使用它初始化静态变量等
    // 静态代码块  优于  普通代码块
    // 普通代码块  优于  构造代码块
    static {
        // 首先获得配置的文件名字
        String fileName = "mmall.properties";
        props = new Properties();
        try {
            // 加载配置文件的实现过程
            props.load(new InputStreamReader(PropertiesUtil.class.getClassLoader().getResourceAsStream(fileName),"UTF-8"));
        } catch (IOException e) {
            logger.error("配置文件读取异常",e);
        }
    }

    // 获取配置文件中的对应key的值。
    public static String getProperty(String key) {
        // 避免key有空格，所以使用trim去掉空格
        String value = props.getProperty(key.trim());
        if(StringUtils.isBlank(value)) {
            return null;
        }

        // 避免value有空格，所以使用trim去掉空格
        return value.trim();
    }

    // 获取配置文件中的对应key的值，如果没有获取到，则返回默认的值
    public static String getProperty(String key,String defaultValue) {
        String value = props.getProperty(key.trim());
        if(StringUtils.isBlank(value)) {
            value = defaultValue;
        }
        return value.trim();
    }
}
