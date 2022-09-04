package com.alibaba.tesla.authproxy.util;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static com.alibaba.tesla.authproxy.Constants.LOCAL_HOST;

/**
 * <p>Description: Cookie处理工具类 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Slf4j
public class CookieUtil {

    private static final String LOCAL_DOMAIN = "127.0.0.1";

    /**
     * 将 request 中的 Cookie 转换为可读字符串
     *
     * @param request
     * @return
     */
    public static String cookieString(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        StringBuilder cookieValue = new StringBuilder();
        if (cookies != null) {
            for (Cookie item : cookies) {
                cookieValue.append(String.format("%s=%s,", item.getName(), item.getValue()));
            }
        } else {
            cookieValue.append("NONE");
        }
        return cookieValue.toString();
    }

    /**
     * 清除登录之后的Cookie信息
     *
     * @param request
     * @param response
     */
    public static void cleanLoginCookie(HttpServletRequest request, HttpServletResponse response, String includName) {
        Cookie[] cookies = request.getCookies();
        if (null != cookies && cookies.length > 0) {
            for (Cookie c : cookies) {
                if (c.getName().contains(includName)) {
                    c.setValue(null);
                    c.setMaxAge(0);
                    response.addCookie(c);
                }
            }
        }
    }

    public static void cleanDomainCookie(HttpServletRequest request, HttpServletResponse response, String includName,
                                         String domain) {
        Cookie[] cookies = request.getCookies();
        if (null != cookies && cookies.length > 0) {
            for (Cookie c : cookies) {
                if (c.getName().contains(includName)) {
                    c.setDomain(domain);
                    c.setValue(null);
                    c.setMaxAge(0);
                    response.addCookie(c);
                }
            }
        }
    }

    public static void cleanDomainCookie(HttpServletRequest request, HttpServletResponse response, String includName,
                                         String domain, String path) {
        Cookie[] cookies = request.getCookies();
        if (null != cookies && cookies.length > 0) {
            for (Cookie c : cookies) {
                if (c.getName().contains(includName)) {
                    if (!LOCAL_DOMAIN.equals(domain)) {
                        c.setDomain(domain);
                    }
                    c.setValue(null);
                    c.setMaxAge(0);
                    c.setPath(Optional.ofNullable(path).orElse("/"));
                    response.addCookie(c);
                }
            }
        }
    }

    /**
     * 添加Cookie
     *
     * @param response
     * @param name
     * @param value
     * @param maxAge
     */
    public static void setCookie(HttpServletResponse response, String name, String value, int maxAge) {
        /**
         * 响应头添加P3P,防止IE浏览器拒绝cookie的问题
         */
        response.setHeader("P3P",
            "CP=\"CURa ADMa DEVa PSAo PSDo OUR BUS UNI PUR INT DEM STA PRE COM NAV OTC NOI DSP COR\"");
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge == 0 ? 24 * 60 * 60 : maxAge);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

    }

    public static void setCookie(HttpServletResponse response, String name, String value, int maxAge, String domain) {
        /**
         * 响应头添加P3P,防止IE浏览器拒绝cookie的问题
         */
        response.setHeader("P3P",
            "CP=\"CURa ADMa DEVa PSAo PSDo OUR BUS UNI PUR INT DEM STA PRE COM NAV OTC NOI DSP COR\"");
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge == 0 ? 24 * 60 * 60 : maxAge);
        cookie.setPath("/");
        cookie.setHttpOnly(false);
        if (!LOCAL_DOMAIN.equals(domain)) {
            cookie.setDomain(domain);
        }
        response.addCookie(cookie);
    }

    /**
     * 获取Cookie信息
     *
     * @param request
     * @param cookieName
     * @return
     */
    public static Cookie getCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (null == cookies || cookies.length == 0) {
            return null;
        }
        Cookie cookie = null;
        for (Cookie c : cookies) {
            if (c.getName().equals(cookieName)) {
                cookie = c;
            }
        }
        return cookie;
    }

    /**
     * 获取指定的 top Domain
     * 针对 Localhost 的方式额外兼容
     */
    public static String getCookieDomain(HttpServletRequest request, String domain) {
        if (LOCAL_HOST.equals(domain)) {
            return request.getServerName();
        }
        return domain;
    }
}
