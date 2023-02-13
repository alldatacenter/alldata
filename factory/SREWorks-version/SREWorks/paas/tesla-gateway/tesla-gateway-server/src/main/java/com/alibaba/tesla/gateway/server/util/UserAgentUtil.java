package com.alibaba.tesla.gateway.server.util;

import com.alibaba.tesla.gateway.server.constants.AuthProxyConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.server.ServerRequest;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class UserAgentUtil {
    private static final String BROWSER_TOKEN = "Mozilla";

    /**
     * 判断是不是浏览器发来的情感求
     * @param request
     * @return
     */
    public static boolean isBrowserReq(ServerHttpRequest request) {
        String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
        if (!StringUtils.isBlank(userAgent) && userAgent.contains(BROWSER_TOKEN)) {
            String xAuthApp = request.getHeaders().getFirst(AuthProxyConstants.HEADER_NAME_APP);
            String xAuthKey = request.getHeaders().getFirst(AuthProxyConstants.HEADER_NAME_KEY);
            String xAuthUser = request.getHeaders().getFirst(AuthProxyConstants.HEADER_NAME_USER);
            String xAuthPwd = request.getHeaders().getFirst(AuthProxyConstants.HEADER_NAME_PASSWD);
            if (StringUtils.isNotBlank(xAuthApp) && StringUtils.isNotBlank(xAuthKey) && StringUtils.isNotBlank(xAuthUser)
                && StringUtils.isNotBlank(xAuthPwd)) {
                return false;
            }
            return true;
        }
        return false;
    }

    public static boolean isBrowserReq(ServerRequest request) {
        String userAgent = request.headers().firstHeader(HttpHeaders.USER_AGENT);
        if (!StringUtils.isBlank(userAgent) && userAgent.contains(BROWSER_TOKEN)) {
            String xAuthApp = request.headers().firstHeader(AuthProxyConstants.HEADER_NAME_APP);
            String xAuthKey = request.headers().firstHeader(AuthProxyConstants.HEADER_NAME_KEY);
            String xAuthUser = request.headers().firstHeader(AuthProxyConstants.HEADER_NAME_USER);
            String xAuthPwd = request.headers().firstHeader(AuthProxyConstants.HEADER_NAME_PASSWD);
            if (StringUtils.isNotBlank(xAuthApp) && StringUtils.isNotBlank(xAuthKey) && StringUtils.isNotBlank(xAuthUser)
                && StringUtils.isNotBlank(xAuthPwd)) {
                return false;
            }
            return true;
        }
        return false;
    }
}
