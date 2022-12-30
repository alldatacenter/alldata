package com.alibaba.sreworks.health.common.exception;

/**
 * 异常类型存在
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class IncidentTypeExistException extends Exception {
    public IncidentTypeExistException(String message) {
        super(message);
    }

    public IncidentTypeExistException(Throwable cause) {
        super(cause);
    }

    public IncidentTypeExistException(String message, Throwable cause) {
        super(message, cause);
    }

}
