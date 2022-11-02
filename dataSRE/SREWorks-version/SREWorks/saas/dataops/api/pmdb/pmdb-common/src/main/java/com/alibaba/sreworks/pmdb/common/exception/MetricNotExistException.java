package com.alibaba.sreworks.pmdb.common.exception;

/**
 * 指标不存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class MetricNotExistException extends Exception {
    public MetricNotExistException(String message) {
        super(message);
    }

    public MetricNotExistException(Throwable cause) {
        super(cause);
    }

    public MetricNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
