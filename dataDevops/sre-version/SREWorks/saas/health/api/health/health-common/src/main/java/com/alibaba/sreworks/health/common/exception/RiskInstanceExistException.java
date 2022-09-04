package com.alibaba.sreworks.health.common.exception;

/**
 * 风险实例存在异常
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 17:36
 */
public class RiskInstanceExistException extends Exception {
    public RiskInstanceExistException(String message) {
        super(message);
    }

    public RiskInstanceExistException(Throwable cause) {
        super(cause);
    }

    public RiskInstanceExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
