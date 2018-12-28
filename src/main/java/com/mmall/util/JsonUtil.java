package com.mmall.util;

import com.alipay.api.internal.util.StringUtils;
import com.google.common.collect.Lists;
import com.mmall.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

@Slf4j
public class JsonUtil {
    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        // 对象的所有字段全部列入
        objectMapper.setSerializationInclusion(Inclusion.ALWAYS);

        // 取消默认转换 timestamps 形式
        objectMapper.configure(SerializationConfig.Feature.WRITE_DATE_KEYS_AS_TIMESTAMPS,false);

        // 忽略空 Bean 转换 json 的错误
        objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS,false);

        // 所有的日期格式都统一为以下的样式，即 yyyy-MM-dd HH:mm:ss
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        // 设置反序列化
        // 忽略在 json 字符串中存在，但是在 java 对象中不存在属性的情况。防止错误
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,false);
    }

    public static <T>String Obj2String(T obj) {
        if(obj == null)
            return null;
        try {
            return obj instanceof String ? (String) obj : objectMapper.writeValueAsString(obj);
        } catch (IOException e) {
            log.warn("Parse object to String error",e);
            return null;
        }
    }

    public static <T>String Obj2StringPretty(T obj) {
        if(obj == null)
            return null;
        try {
            return obj instanceof String ? (String) obj : objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (IOException e) {
            log.warn("Parse object to String error",e);
            return null;
        }
    }

    public static <T> T String2Obj(String str, Class<T> clazz) {
        if(StringUtils.isEmpty(str) || clazz == null)
            return null;
        try {
            return clazz.equals(String.class) ? (T) str : objectMapper.readValue(str, clazz);
        } catch (IOException e) {
            log.warn("Parse String to obj error",e);
            return null;
        }
    }

    // 泛型反序列化
    public static <T> T String2Obj(String str, TypeReference<T> typeReference) {
        if(StringUtils.isEmpty(str) || typeReference == null) {
            return null;
        }
        try {
            return (T) (typeReference.getType().equals(String.class) ? str : objectMapper.readValue(str, typeReference));
        } catch (IOException e) {
            log.warn("parse String to obj error",e);
            return null;
        }
    }

    // 另一种反序列化
    public static <T> T String2Obj(String str, Class<?> collectionClass, Class<?> elementClasses) {
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(collectionClass,elementClasses);
        try {
            return objectMapper.readValue(str, javaType);
        } catch (IOException e) {
            log.warn("parse String to obj error",e);
            return null;
        }
    }

    public static void main(String[] args) {
        User user = new User();
        user.setUsername("fj");
        user.setEmail("4577332323@qq.com");

        User user2 = new User();
        user2.setUsername("cuipanger");
        user2.setEmail("4577@qq.com");

        String user1JSon = JsonUtil.Obj2String(user);
        String user1PrettyJSon = JsonUtil.Obj2StringPretty(user);

        log.info("userjson:{}",user1JSon);
        log.info("user1PrettyJSon:{}",user1PrettyJSon);

        User u2 = JsonUtil.String2Obj(user1JSon,User.class);
        log.info("u2:{}",u2);

        List<User> userList = Lists.newArrayList();
        userList.add(user);
        userList.add(user2);

        String userListStr = JsonUtil.Obj2StringPretty(userList);

        List<User> userList1 = JsonUtil.String2Obj(userListStr, new TypeReference<List<User>>() {
        });

        List<User> userList2 = JsonUtil.String2Obj(userListStr,List.class, User.class);


    }
}