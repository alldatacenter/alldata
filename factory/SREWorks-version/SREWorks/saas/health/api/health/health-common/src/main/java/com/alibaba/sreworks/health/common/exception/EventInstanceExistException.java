package com.alibaba.sreworks.health.common.exception;

/**
 * 事件实例存在异常
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 17:36
 */
public class EventInstanceExistException extends Exception {
    public EventInstanceExistException(String message) {
        super(message);
    }

    public EventInstanceExistException(Throwable cause) {
        super(cause);
    }

    public EventInstanceExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
