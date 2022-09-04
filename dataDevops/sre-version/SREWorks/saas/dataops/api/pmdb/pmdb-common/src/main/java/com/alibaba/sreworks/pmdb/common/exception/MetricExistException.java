package com.alibaba.sreworks.pmdb.common.exception;

/**
 * 指标存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class MetricExistException extends Exception {
    public MetricExistException(String message) {
        super(message);
    }

    public MetricExistException(Throwable cause) {
        super(cause);
    }

    public MetricExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
