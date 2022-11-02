package com.alibaba.tesla.authproxy.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 日志工具类
 */
@Slf4j
public class LogUtil {

    /**
     * 组合 action 名称到 LOG PRE
     *
     * @param actionName action 名称
     * @return
     */
    public static String action(String actionName) {
        return "action=" + actionName + "||";
    }
}
