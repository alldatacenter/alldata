package com.alibaba.tesla.authproxy.interceptor;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.model.AppDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.util.AuthUtil;
import com.alibaba.tesla.authproxy.util.StringUtil;
import com.alibaba.tesla.common.utils.TeslaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * SSO 登录拦截过滤器
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Component("ssoLoginInterceptor")
@Slf4j
public class SsoLoginInterceptor implements LoginInterceptor {

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private AuthUtil authUtil;

    /**
     * SSO 登录拦截器
     */
    @Override
    public boolean interceptor(HttpServletRequest request, HttpServletResponse response) {
        return true;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, String callback) {
        String logoutCallbackUrl = authProperties.getAuthproxyDomain() + "/bucSSOLogout.do?" +
            Constants.REFERER_URL + "=" + callback;
        String ssoLogoutUrl;
        try {
            logoutCallbackUrl = URLEncoder.encode(logoutCallbackUrl, "UTF-8");
            ssoLogoutUrl = authProperties.getLogoutUrl() + "&BACK_URL=" + logoutCallbackUrl;
        } catch (UnsupportedEncodingException e) {
            log.error("SSO登出回调地址URLEndode编码错误");
            throw new ApplicationException(TeslaResult.FAILURE, "error.logout");
        }
        log.info("Redirect to sso logout page {}" + ssoLogoutUrl);
        try {
            response.sendRedirect(ssoLogoutUrl);
        } catch (Exception ignored) {}
    }

    /**
     * 获取当前的登录地址
     */
    private String buildLoginUrl(HttpServletRequest request, AppDO appDo) throws UnsupportedEncodingException {
        String referer = request.getParameter("callbackUrl");
        if (null == referer) {
            referer = request.getHeader(HttpHeaders.REFERER);
        }
        if (StringUtil.isEmpty(referer)) {
            // 返回指定 app 的主页
            if (null != appDo) {
                referer = appDo.getIndexUrl();
            }
            if (StringUtil.isEmpty(referer)) {
                referer = authProperties.getCallbackUrl();
            }
        }

        // 生成 Login URL
        String encodingRefererUrl = URLEncoder.encode(referer, "UTF-8");
        String toUrl = authProperties.getCallbackUrl() + "?" + Constants.REFERER_URL + "=" + encodingRefererUrl;
        return authProperties.getLoginUrl() + "&BACK_URL=" + URLEncoder.encode(toUrl, "UTF-8");
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
