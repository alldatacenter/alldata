package com.alibaba.sreworks.health.common.exception;

/**
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 17:36
 */
public class AlertInstanceExistException extends Exception {
    public AlertInstanceExistException(String message) {
        super(message);
    }

    public AlertInstanceExistException(Throwable cause) {
        super(cause);
    }

    public AlertInstanceExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
