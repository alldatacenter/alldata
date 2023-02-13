package com.alibaba.tesla.authproxy.interceptor;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.service.AuthPolicy;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>Title: AuthProxyInterceptor.java<／p>
 * <p>Description: SpringMVC拦截器 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Component("authProxyInterceptor")
public class AuthProxyInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthPolicy authPolicy;

    @Autowired
    private AuthProperties authProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String url = request.getRequestURI();
        if (matchRequest(url)) {
            return true;
        }

        // 调用登录拦截器，登录成功之后放行
        LoginInterceptor tli = this.authPolicy.getLoginServiceManager();
        return tli.interceptor(request, response);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {}

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {}

    private static AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 过滤不拦截的请求path
     *
     * @param url
     * @return
     */
    private boolean matchRequest(String url) {
        String exclusions = authProperties.getExclusionUrl();
        String[] exclus = exclusions.split(",");
        if (ArrayUtils.contains(exclus, url)) {
            return true;
        } else {
            for (String ex : exclus) {
                boolean res = StringUtils.isNotBlank(ex) && pathMatcher.match(ex, url);
                if(res){
                    return true;
                }
            }
        }
        return false;
    }
}