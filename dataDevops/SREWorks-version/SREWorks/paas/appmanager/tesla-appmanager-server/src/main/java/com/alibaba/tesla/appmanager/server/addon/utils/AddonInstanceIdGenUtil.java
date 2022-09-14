package com.alibaba.tesla.appmanager.server.addon.utils;

import java.util.UUID;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class AddonInstanceIdGenUtil {

    private AddonInstanceIdGenUtil() {
    }

    public static String genInstanceId() {
        return UUID.randomUUID().toString();
    }
}
