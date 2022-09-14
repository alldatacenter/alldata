package com.alibaba.sreworks.health.common.exception;

/**
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 17:36
 */
public class AlertInstanceNotExistException extends Exception {
    public AlertInstanceNotExistException(String message) {
        super(message);
    }

    public AlertInstanceNotExistException(Throwable cause) {
        super(cause);
    }

    public AlertInstanceNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
