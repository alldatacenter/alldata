package com.alibaba.sreworks.health.common.exception;

/**
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 17:36
 */
public class IncidentInstanceNotExistException extends Exception {
    public IncidentInstanceNotExistException(String message) {
        super(message);
    }

    public IncidentInstanceNotExistException(Throwable cause) {
        super(cause);
    }

    public IncidentInstanceNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}