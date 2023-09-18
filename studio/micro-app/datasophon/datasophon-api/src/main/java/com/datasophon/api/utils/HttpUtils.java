package com.datasophon.api.utils;

import com.datasophon.common.Constants;
import org.apache.commons.lang.StringUtils;
import javax.servlet.http.HttpServletRequest;


public class HttpUtils {

    public static String getClientIpAddress(HttpServletRequest request) {
        String clientIp = request.getHeader(Constants.HTTP_X_FORWARDED_FOR);

        if (StringUtils.isNotEmpty(clientIp) && !clientIp.equalsIgnoreCase(Constants.HTTP_HEADER_UNKNOWN)) {
            int index = clientIp.indexOf(Constants.COMMA);
            if (index != -1) {
                return clientIp.substring(0, index);
            } else {
                return clientIp;
            }
        }

        clientIp = request.getHeader(Constants.HTTP_X_REAL_IP);
        if (StringUtils.isNotEmpty(clientIp) && !clientIp.equalsIgnoreCase(Constants.HTTP_HEADER_UNKNOWN)) {
            return clientIp;
        }

        return request.getRemoteAddr();
    }
}
