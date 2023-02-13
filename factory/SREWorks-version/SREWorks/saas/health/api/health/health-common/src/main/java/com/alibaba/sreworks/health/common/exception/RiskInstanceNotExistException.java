package com.alibaba.sreworks.health.common.exception;

/**
 * 风险实例不存在异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/04 10:22
 */
public class RiskInstanceNotExistException extends Exception {
    public RiskInstanceNotExistException(String message) {
        super(message);
    }

    public RiskInstanceNotExistException(Throwable cause) {
        super(cause);
    }

    public RiskInstanceNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
