package com.alibaba.tesla.authproxy.util;

import com.alibaba.tesla.authproxy.exceptions.TeslaUserArgsException;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class RequestUtil {

    /**
     * 从URL中获取指定名称的参数值
     *
     * @param url  URL
     * @param name 参数名称
     * @return
     */
    public static String getParamValue(String url, String name) {
        String params = url.substring(url.indexOf("?") + 1, url.length());
        if (StringUtils.isEmpty(params) || params.equals(url)) {
            return "";
        } else {
            Map<String, String> split = Splitter.on("&").withKeyValueSeparator("=").split(params);
            String result = split.get(name);
            if (null == result) {
                return "";
            }
            return result;
        }
    }

    /**
     * 从 Http 请求中获取参数信息，优先级 URL Parameters -> Headers
     * @param request 请求
     * @return value
     */
    public static String getParameterValue(HttpServletRequest request, String key) throws TeslaUserArgsException {
        String value = request.getParameter(key);
        if (!StringUtils.isEmpty(value)) {
            return value;
        }
        value = request.getHeader(key);
        if (!StringUtils.isEmpty(value)) {
            return value;
        }
        Map<String, String> errorMessages = new HashMap<>();
        errorMessages.put(key, "required field");
        throw new TeslaUserArgsException(errorMessages);
    }

    /**
     * 从 Http 请求中获取 App Id 信息，优先级 URL Parameters -> Headers
     * @param request 请求
     * @return value
     */
    public static String getAppId(HttpServletRequest request) throws TeslaUserArgsException {
        String appId = request.getParameter("appId");
        if (!StringUtils.isEmpty(appId)) {
            return appId;
        }
        appId = request.getHeader("X-Auth-App");
        if (!StringUtils.isEmpty(appId)) {
            return appId;
        }
        Map<String, String> errorMessages = new HashMap<>();
        errorMessages.put("appId", "required field");
        throw new TeslaUserArgsException(errorMessages);
    }
}
