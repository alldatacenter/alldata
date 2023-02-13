package com.alibaba.sreworks.health.common.exception;

/**
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 17:36
 */
public class IncidentInstanceExistException extends Exception {
    public IncidentInstanceExistException(String message) {
        super(message);
    }

    public IncidentInstanceExistException(Throwable cause) {
        super(cause);
    }

    public IncidentInstanceExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
