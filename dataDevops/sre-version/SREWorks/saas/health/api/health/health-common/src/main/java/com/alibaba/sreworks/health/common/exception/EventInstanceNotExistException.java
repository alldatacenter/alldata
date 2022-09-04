package com.alibaba.sreworks.health.common.exception;

/**
 * 事件实例不存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/04 10:22
 */
public class EventInstanceNotExistException extends Exception {
    public EventInstanceNotExistException(String message) {
        super(message);
    }

    public EventInstanceNotExistException(Throwable cause) {
        super(cause);
    }

    public EventInstanceNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
