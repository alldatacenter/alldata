package com.alibaba.tesla.tkgone.server.common;

import org.slf4j.Logger;

/**
 * 常用错误处理工具
 * @author feiquan
 */
public class ErrorUtil {
    /**
     * 输出异常信息
     * @param e 异常
     * @param message 消息，支持替占位符
     * @param args 结果变量
     */
    public static void log(Logger logger, Throwable e, String message, Object...args) {
        logger.warn(appendError(message, e), args);
        if (e instanceof NullPointerException
            || e instanceof IndexOutOfBoundsException) {
            logger.warn("error detail: ", e);
        } else if (logger.isDebugEnabled()) {
            logger.debug("error detail: ", e);
        }
    }

    private static String appendError(String message, Throwable e) {
        return message + " error: " + e.getClass().getSimpleName()
            + " msg: " + e.getMessage();
    }
}
