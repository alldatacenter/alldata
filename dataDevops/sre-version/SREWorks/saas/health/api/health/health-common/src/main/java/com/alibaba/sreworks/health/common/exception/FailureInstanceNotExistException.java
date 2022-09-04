package com.alibaba.sreworks.health.common.exception;

/**
 * 故障实例不存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/04 10:22
 */
public class FailureInstanceNotExistException extends Exception {
    public FailureInstanceNotExistException(String message) {
        super(message);
    }

    public FailureInstanceNotExistException(Throwable cause) {
        super(cause);
    }

    public FailureInstanceNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
