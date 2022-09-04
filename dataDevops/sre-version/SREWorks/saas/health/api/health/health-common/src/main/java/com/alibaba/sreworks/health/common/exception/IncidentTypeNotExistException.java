package com.alibaba.sreworks.health.common.exception;

/**
 * 异常类型不存在
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class IncidentTypeNotExistException extends Exception {
    public IncidentTypeNotExistException(String message) {
        super(message);
    }

    public IncidentTypeNotExistException(Throwable cause) {
        super(cause);
    }

    public IncidentTypeNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

}
