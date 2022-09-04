package com.alibaba.tesla.gateway.common.utils;

import org.apache.commons.lang3.Validate;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class ServerNameCheckUtil {

    public static final String SERVER_NAME_PREFIX = "lb://";

    private ServerNameCheckUtil() {
    }

    /**
     * 服务名只能包含数字、字母、点和中划线
     * @param serverName serverName
     */
    public static void check(String serverName){
        Validate.notBlank(serverName);
        serverName = serverName.substring(SERVER_NAME_PREFIX.length()).split("/")[0];
        for (int i = 0; i < serverName.length(); i++) {
            char ch = serverName.charAt(i);
            if(Character.isLetterOrDigit(ch) || ch == '-' || ch == '.'){
                continue;
            }
            throw new IllegalArgumentException("server name only contain alphabet(a-z), digits(0-9), minus sign(-), and period(.).");
        }
    }
}
