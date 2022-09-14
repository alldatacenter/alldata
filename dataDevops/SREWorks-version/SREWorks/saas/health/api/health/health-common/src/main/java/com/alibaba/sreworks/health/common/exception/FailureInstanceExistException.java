package com.alibaba.sreworks.health.common.exception;

/**
 * 故障实例存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/04 10:22
 */
public class FailureInstanceExistException extends Exception {
    public FailureInstanceExistException(String message) {
        super(message);
    }

    public FailureInstanceExistException(Throwable cause) {
        super(cause);
    }

    public FailureInstanceExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
