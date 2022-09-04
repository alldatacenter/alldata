package com.alibaba.tesla.authproxy.interceptor;

import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登陆拦截器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface LoginInterceptor {

    /**
     * 登录拦截器处理
     *
     * @param request
     * @param response
     * @return
     */
    boolean interceptor(HttpServletRequest request, HttpServletResponse response) throws AuthProxyException;

    /**
     * 登出当前用户
     *
     * @param request  请求
     * @param response 响应
     * @param callback 回调地址
     */
    void logout(HttpServletRequest request, HttpServletResponse response, String callback);
}
