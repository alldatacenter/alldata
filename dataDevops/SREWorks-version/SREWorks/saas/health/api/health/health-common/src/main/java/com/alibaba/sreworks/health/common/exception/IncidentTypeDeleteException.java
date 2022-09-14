package com.alibaba.sreworks.health.common.exception;

/**
 * 异常类型删除异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class IncidentTypeDeleteException extends Exception {
    public IncidentTypeDeleteException(String message) {
        super(message);
    }

    public IncidentTypeDeleteException(Throwable cause) {
        super(cause);
    }

    public IncidentTypeDeleteException(String message, Throwable cause) {
        super(message, cause);
    }

}
