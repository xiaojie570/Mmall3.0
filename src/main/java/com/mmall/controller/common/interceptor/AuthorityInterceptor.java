package com.mmall.controller.common.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by lenovo on 2018/12/31.
 */

@Slf4j
public class AuthorityInterceptor implements HandlerInterceptor{
    // 在Controller之前
    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handler) throws Exception {
        log.info("preHandle");
        System.out.println("==================================================================================================================pre==============================================");
        // 请求中 Controller 中的方法名字
        // 解析 HandlerMehtod
        // 解析参数，具体的参数 key 以及 value 是什么， 我们打印日志

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
