package com.alibaba.tesla.authproxy.interceptor;

import com.alibaba.tesla.authproxy.AuthProxyClientProperties;
import com.alibaba.tesla.authproxy.AuthProxyConstant;
import com.alibaba.tesla.authproxy.api.AuthProxyClient;
import com.alibaba.tesla.authproxy.api.dto.TeslaUserInfoDo;
import com.alibaba.tesla.authproxy.api.exception.AuthProxyUnauthorizedException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnClass(AuthProxyClient.class)
@EnableConfigurationProperties(AuthProxyClientProperties.class)
public class AuthProxyAuthorizedInterceptor extends HandlerInterceptorAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AuthProxyAuthorizedInterceptor.class);

    @Autowired
    private AuthProxyClient client;

    @Autowired
    private AuthProxyClientProperties properties;

    private Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        TeslaUserInfoDo userInfo;
        try {
            userInfo = client.getUserInfo(properties.getAppId(), request);
        } catch (AuthProxyUnauthorizedException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "Unauthorized");
            result.put("data", null);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(result));
            return false;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", e.getMessage());
            result.put("data", null);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(result));
            return false;
        }
        request.setAttribute(AuthProxyConstant.AUTHPROXY_USERINFO, userInfo);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {}

}
