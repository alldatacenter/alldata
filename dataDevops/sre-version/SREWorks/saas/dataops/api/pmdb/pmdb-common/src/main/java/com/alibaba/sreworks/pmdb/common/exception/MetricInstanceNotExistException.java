package com.alibaba.sreworks.pmdb.common.exception;

/**
 * 指标实例不存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class MetricInstanceNotExistException extends Exception {
    public MetricInstanceNotExistException(String message) {
        super(message);
    }

    public MetricInstanceNotExistException(Throwable cause) {
        super(cause);
    }

    public MetricInstanceNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
