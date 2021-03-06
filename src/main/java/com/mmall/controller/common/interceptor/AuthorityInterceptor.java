package com.mmall.controller.common.interceptor;

import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisShardedPoolUtill;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
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
        String className = handlerMethod.getBean().getClass().getSimpleName();   // 获取的是对应的类名字

        // 解析参数，具体的参数 key 以及 value 是什么， 我们打印日志
        StringBuilder requestParamBuffer = new StringBuilder();
        Map paramMap = httpServletRequest.getParameterMap();  // 获取的是参数名字和参数值
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

        // ============================================================================================================
        // 解决循环依赖问题
        if(StringUtils.equals(className,"UserManageController") && StringUtils.equals(methodName,"login")) {
            log.info("权限拦截器拦截到请求，className:{},methodName:{}",className,methodName);
            // 如果是拦截到登录请求，不打印参数，如果参数里面有密码，全部会打印到日志中，防止日志泄漏
            return true;
        }
        // ============================================================================================================

        User user = null;

        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if(StringUtils.isNotEmpty(loginToken)) {
            String userJson = RedisShardedPoolUtill.get(loginToken);
            user = JsonUtil.String2Obj(userJson,User.class);
        }

        if(user == null || (user.getRole().intValue() != Const.Role.ROLE_ADMIN)) {
            // 返回false， 即不会调用Controller 里面的方法
            httpServletResponse.reset(); // 这里要添加 reset，否则会报一个异常: getWriter() has already been called for this response.
            httpServletResponse.setCharacterEncoding("UTF-8");   // 这里要设置编码，否则会乱码
            httpServletResponse.setContentType("application/json;charset=UTF-8");  // 这里要设置返回值的类型，因为都是json接口

            // 拿到response的输出对象
            PrintWriter printWriter = httpServletResponse.getWriter();


            // 上传由于富文本的空间要求，要特殊处理返回值，这里面区分是否登录以及是否有权限。
            if(user == null) {
                if(StringUtils.equals(className,"ProductManageController") && StringUtils.equals(methodName,"richtextImgUpload")) {
                    Map resultMap = Maps.newHashMap();
                    resultMap.put("success",false);
                    resultMap.put("msg","请登录管理员");
                    printWriter.print(JsonUtil.Obj2String(resultMap));
                } else {
                    printWriter.print(JsonUtil.Obj2String(ServerResponse.createByErrorMessage("拦截器，用户还未登录")));
                }
            } else {
                if(StringUtils.equals(className,"ProductManageController") && StringUtils.equals(methodName,"richtextImgUpload")) {
                    Map resultMap = Maps.newHashMap();
                    resultMap.put("success",false);
                    resultMap.put("msg","无权限操作");
                    printWriter.print(JsonUtil.Obj2String(resultMap));
                } else {
                    printWriter.print(JsonUtil.Obj2String(ServerResponse.createByErrorMessage("拦截器，用户不是管理员权限")));
                }
            }

            printWriter.flush();
            printWriter.close(); // 这里要关闭

            return false;
        }
        return true;
    }

    // 在Controller之后
    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {
        log.info("postHandle");

    }

    // 在所有处理完之后调用的，
    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {
        log.info("afterCompletion");
    }
}
