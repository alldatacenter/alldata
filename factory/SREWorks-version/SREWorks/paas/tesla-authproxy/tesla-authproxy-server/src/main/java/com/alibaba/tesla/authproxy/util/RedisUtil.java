package com.alibaba.tesla.authproxy.util;

/**
 * Redis 工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class RedisUtil {

    public static String buildPrivateTicketKey(String aliyunTicket) {
        return "authproxy:ticket:" + aliyunTicket;
    }

}
