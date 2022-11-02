package com.alibaba.sreworks.pmdb.common.exception;

/**
 * 指标实例存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class MetricInstanceExistException extends Exception {
    public MetricInstanceExistException(String message) {
        super(message);
    }

    public MetricInstanceExistException(Throwable cause) {
        super(cause);
    }

    public MetricInstanceExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
