package com.mmall.controller.common.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by lenovo on 2018/12/31.
 */

@Slf4j
public class AuthorityInterceptor implements HandlerInterceptor{
    // 在Controller之前
    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handler) throws Exception {
        log.info("preHandle");
        // 请求中 Controller 中的方法名字
        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 解析 HandlerMehtod
        String methodName = handlerMethod.getMethod().getName();   // 获取方法名字
        String className = handlerMethod.getBean().getClass().getSimpleName();   // 获取类名

        // 解析参数，具体的参数 key 以及 value 是什么， 我们打印日志
        StringBuilder requestParamBuffer = new StringBuilder();
        Map paramMap = httpServletRequest.getParameterMap();
        Iterator iterator = paramMap.entrySet().iterator();
        while( iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String mapKey = (String) entry.getKey();

            String mapValue = StringUtils.EMPTY;

            // request 这个参数的 map， 里面的value 返回的是一个 String[]
            Object object = entry.getValue();
            if(object instanceof String[]) {
                String[] strs = (String[]) object;
                mapValue = Arrays.toString(strs);
            }
            requestParamBuffer.append(mapKey).append("=").append(mapValue);
        }

        return true;
    }

    // 在Controller之后
    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {
        log.info("postHandle");
        System.out.println("==================================================================================================================post==============================================");

    }

    // 在所有处理完之后调用的，
    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {
        log.info("afterCompletion");
    }
}
