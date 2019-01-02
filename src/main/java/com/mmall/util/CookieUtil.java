package com.mmall.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class CookieUtil {
    private final static String COOKIE_DOMAIN = "nginx.tomcat.com";
    private final static String COOKIE_NAME = "mmall_login_token";

    public static String readLoginToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for(Cookie ck: cookies) {
                log.info("read cookieName:{}. cookieValue:{}", ck.getName(),ck.getValue());
                if(StringUtils.equals(ck.getName(),COOKIE_NAME)) {
                    log.info("return cookieName:{}, cookieValue:{}",ck.getName(),ck.getValue());
                    return ck.getValue();
                }
            }
        }
        return null;
    }

    public static void writeLoginToken(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(COOKIE_NAME,token);
        cookie.setDomain(COOKIE_DOMAIN);
        cookie.setPath("/");

        // 但是为秒
        // 如果这个maxAge不设置的话，cookie 就不会写入硬盘，而是写在内存中。只有当前的页面有效。
        cookie.setMaxAge(60 * 60 * 24 * 365);

        log.info("write cookeName:{}, cookieValue:{}",cookie.getName(),cookie.getValue());

        response.addCookie(cookie);
    }

    public static void delLoginToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for(Cookie ck : cookies) {
                log.info("del cookieName:{}, cookieValue:{}",ck.getName(),ck.getValue());
                if(StringUtils.equals(ck.getName(),COOKIE_NAME)) {
                    log.info("del cookieName:{}, cookieValue:{}",ck.getName(),ck.getValue());
                    ck.setDomain(COOKIE_DOMAIN);
                    ck.setPath("/");
                    ck.setMaxAge(0); // 设置成0，代表删除此 cookie
                    response.addCookie(ck);
                    return;
                }
            }
        }
    }
}