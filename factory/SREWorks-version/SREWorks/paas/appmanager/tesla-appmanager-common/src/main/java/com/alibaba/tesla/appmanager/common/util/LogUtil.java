package com.alibaba.tesla.appmanager.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * 节点日志打印工具类
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
public class LogUtil {

    private static final String LOG_INFO = "[ DAG ] instanceId=%s, nodeId=%s, nodeName=%s, %s";

    public static void info(Long instanceId, Long nodeId, String nodeName, String msg) {
        log.info(String.format(LOG_INFO, instanceId, nodeId, nodeName, msg));
    }

    public static void warn(Long instanceId, Long nodeId, String nodeName, String msg) {
        log.warn(String.format(LOG_INFO, instanceId, nodeId, nodeName, msg));
    }

    public static void error(Long instanceId, Long nodeId, String nodeName, String msg) {
        log.error(String.format(LOG_INFO, instanceId, nodeId, nodeName, msg));
    }

    public static void error(Long instanceId, Long nodeId, String nodeName, Exception ex) {
        log.error(String.format(LOG_INFO, instanceId, nodeId, nodeName, ExceptionUtils.getStackTrace(ex)));
    }

}
