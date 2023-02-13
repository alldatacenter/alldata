package com.alibaba.tesla.appmanager.spring.util;

import com.alibaba.tesla.web.constant.HttpHeaderNames;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * HTTPServletRequest 常用处理工具
 *
 * @author dongdong.ldd@alibaba-inc.com, yaoxing.gyx@alibaba-inc.com
 */
public class ServletRequestUtils {

    private static final String URI_REDIRECTION = "REDIRECTION";

    private static final String URI_NOT_FOUND = "NOT_FOUND";

    private static final String URI_ROOT = "root";

    private static final String URI_UNKNOWN = "UNKNOWN";

    /**
     * 获取HTTP请求的完整路径，包括URL和参数
     */
    public static String getFullUrl(HttpServletRequest request) {
        if (null == request) {
            return "";
        }
        if (StringUtils.isEmpty(request.getQueryString())) {
            return request.getRequestURL().toString();
        } else {
            return request.getRequestURL().append('?').append(request.getQueryString()).toString();
        }
    }

    /**
     * 获取客户端 IP 地址
     * <p>
     * From: https://www.atatech.org/articles/28714
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = null;
        if (request.getHeader(HttpHeaderNames.ORIG_CLIENT_IP) != null) {
            ip = request.getHeader(HttpHeaderNames.ORIG_CLIENT_IP);
        }
        if (ip == null && request.getHeader(HttpHeaderNames.X_FORWARDED_FOR) != null) {
            ip = request.getHeader(HttpHeaderNames.X_FORWARDED_FOR);

            if (ip != null) {
                ip = ip.split("\\s*,\\s*")[0];
            }
        }
        if (ip == null) {
            ip = request.getHeader(HttpHeaderNames.CLIENT_IP);
        }
        if (ip == null) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 获取指定 request + response 对应的 uri 配对 pattern
     *
     * @param request  请求
     * @param response 响应
     * @return 配对 pattern
     */
    public static String requestPattern(HttpServletRequest request, HttpServletResponse response) {
        if (request != null) {
            String pattern = getMatchingPattern(request);
            if (pattern != null) {
                return pattern;
            } else if (response != null) {
                HttpStatus status = extractStatus(response);
                if (status != null && status.is3xxRedirection()) {
                    return URI_REDIRECTION;
                }
                if (status != null && status.equals(HttpStatus.NOT_FOUND)) {
                    return URI_NOT_FOUND;
                }
            }
            String pathInfo = getPathInfo(request);
            if (pathInfo.isEmpty()) {
                return URI_ROOT;
            }
        }
        return URI_UNKNOWN;
    }

    private static String getMatchingPattern(HttpServletRequest request) {
        return (String)request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    }

    private static String getPathInfo(HttpServletRequest request) {
        String uri = StringUtils.hasText(request.getPathInfo()) ?
                request.getPathInfo() : "/";
        return uri.replaceAll("//+", "/")
                .replaceAll("/$", "");
    }

    private static HttpStatus extractStatus(HttpServletResponse response) {
        try {
            return HttpStatus.valueOf(response.getStatus());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}