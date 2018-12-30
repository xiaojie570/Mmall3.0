package com.mmall.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJacksonJsonView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.handler.Handler;

/**
 * Created by lenovo on 2018/12/30.
 */

@Slf4j
public class ExceptionResolver implements HandlerExceptionResolver{
    /**
     * 1. 首先使用log 打印出异常日志
     * 2. 将ModelAndView 转换为一个 jsonView
     * 3. 当使用的是jackson2.x 的时候使用MappingJackson2JsonView
     * @param httpServletRequest
     * @param httpServletResponse
     * @param o 具体的handler
     * @param e
     * @return
     */

    @Override
    public ModelAndView resolveException(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) {
        log.error("{} Exception ",httpServletRequest.getRequestURI(),e);
        ModelAndView modelAndView = new ModelAndView(new MappingJacksonJsonView());
        modelAndView.addObject("status",ResponseCode.ERROR.getCode());
        modelAndView.addObject("msg","接口异常，详情请查看服务器日志");
        modelAndView.addObject("data",e.toString());
        return modelAndView;
    }
}
