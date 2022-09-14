package com.alibaba.sreworks.common.util;

import java.net.InetAddress;
import java.util.UUID;

/**
 * @author jinghua.yjh
 */
public class ClientUtil {

    public static String localIp = localIp();

    public static String localClient = localClient();

    private static String localIp() {
        String ip;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            ip = addr.getHostAddress();
        } catch (Exception ex) {
            ip = "";
        }
        return ip;
    }

    private static String localClient() {
        return localIp + "_" + UUID.randomUUID().toString().replace("-", "");
    }

}
