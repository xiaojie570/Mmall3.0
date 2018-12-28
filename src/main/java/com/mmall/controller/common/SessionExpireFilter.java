package com.mmall.controller.common;

import com.mmall.common.Const;
import com.mmall.pojo.User;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisPoolUtill;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by lenovo on 2018/12/28.
 */
public class SessionExpireFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);

        if(StringUtils.isNotEmpty(loginToken)) {
            // 判断loginToken是否为空或者“”
            // 如果不为空的话，即符合条件，继续拿User的信息
            String userJsonStr = RedisPoolUtill.get(loginToken);
            User user = JsonUtil.String2Obj(userJsonStr,User.class);

            if(user != null) {
                // 如果user不为空，则重置 session的时间，即调用redis的expire命令
                RedisPoolUtill.expire(loginToken, Const.RedisCacheExtime.REDIS_SESSION_EXTIME);
            }
            chain.doFilter(httpServletRequest,response);
        }
    }

    @Override
    public void destroy() {

    }
}
