package com.alibaba.tesla.authproxy.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.model.mapper.AppMapper;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.service.TeslaUserService;
import com.alibaba.tesla.authproxy.util.*;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 数据库认证拦截器
 * @author tandong
 * @Description:TODO
 * @date 2019/3/20 21:11
 */
@Component
@Slf4j
public class DataBaseLoginInterceptor implements LoginInterceptor {

    @Autowired
    AuthProperties authProperties;

    @Autowired
    AppMapper appMapper;

    @Autowired
    TeslaUserService teslaUserService;

    @Autowired
    AuthUtil authUtil;

    @Override
    public boolean interceptor(HttpServletRequest request, HttpServletResponse response) {
        log.info("interceptor {}", request.getRequestURI());
        // 检查是否存在合法的第三方应用 Header，存在则直接放行
        UserDO teslaUser = getExtAppUser(request);
        if (null != teslaUser) {
            request.setAttribute(Constants.REQUEST_ATTR_USER, teslaUser);
            return true;
        }

        // 检查 Cookie
        Cookie cookieToken = CookieUtil.getCookie(request, Constants.COOKIE_DATABASE_LOGIN_TOKEN);
        if(null != cookieToken){
            log.info("cookieToken:{}", JSONObject.toJSONString(cookieToken));
        }

        Cookie cookieLoginName = CookieUtil.getCookie(request, Constants.COOKIE_DATABASE_LOGIN_USER_ID);
        if(null != cookieLoginName){
            log.info("cookieLoginName:{}", JSONObject.toJSONString(cookieLoginName));
        }

        if (null != cookieToken) {
            log.info("{} Get database cookie info, loginToken={}:{}", request.getRequestURI(), cookieToken.getName(), cookieToken.getValue());
            Claims claims = JwtUtil.verify(cookieToken.getValue(), authProperties.getOauth2JwtSecret());
            if(null == claims){
                log.warn("!!!!!!Cookie verification failed, claims is empty.");
                ResponseUtil.writeNoLoginJson(response, "", "");
                return false;
            }
            UserDO loginUser = teslaUserService.getUserByLoginName(String.valueOf(claims.get("tesla_user_id")));
            if(null == loginUser){
                log.warn("!!!!!!Cookie verification failed, login user not exits.");
                ResponseUtil.writeNoLoginJson(response, "", "");
                return false;
            }
            return true;
        }

        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                log.info("cookie value = {}", JSONObject.toJSONString(c));
            }
        }
        ResponseUtil.writeNoLoginJson(response, "", "");
        return false;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, String callback) {
        String cookieDomain = authProperties.getCookieDomain();
        String topDomain = CookieUtil.getCookieDomain(request, cookieDomain);
        CookieUtil.cleanDomainCookie(request, response, Constants.COOKIE_DATABASE_LOGIN_TOKEN, topDomain, "/");
        CookieUtil.cleanDomainCookie(request, response, Constants.COOKIE_DATABASE_LOGIN_USER_ID, topDomain, "/");
        CookieUtil.cleanDomainCookie(request, response, Constants.COOKIE_LANG, topDomain, "/");
        CookieUtil.cleanDomainCookie(request, response, Constants.COOKIE_COUNTRY, topDomain, "/");

        String redirectUrl = authProperties.getNetworkProtocol() + "://" + cookieDomain;
        String homeUrl = System.getenv("HOME_URL");
        if (StringUtils.isNotEmpty(homeUrl)) {
            redirectUrl = homeUrl;
        }
        try {
            response.sendRedirect(redirectUrl);
        } catch (Exception ignored) {}
    }

    /**
     * 检查是否携带第三方 APP Header，如果携带则检查合法性
     *
     * @param request HTTP 请求
     * @return 合法则返回 true
     */
    private UserDO getExtAppUser(HttpServletRequest request) {
        String authApp = request.getHeader("x-auth-app");
        String authKey = request.getHeader("x-auth-key");
        String authUser = request.getHeader("x-auth-user");
        String authPassword = request.getHeader("x-auth-passwd");
        // fakePassword 为了兼容曾经的老版本的错误 Header 头名称
        String authFakePassword = request.getHeader("x-auth-password");
        if (!StringUtil.isEmpty(authFakePassword)) {
            authPassword = authFakePassword;
        }
        return authUtil.getExtAppUser(authApp, authKey, authUser, authPassword);
    }
}
