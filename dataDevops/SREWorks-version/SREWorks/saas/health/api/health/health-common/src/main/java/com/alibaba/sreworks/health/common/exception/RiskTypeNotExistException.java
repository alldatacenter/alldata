package com.alibaba.sreworks.health.common.exception;

/**
 * 风险类型不存在
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class RiskTypeNotExistException extends Exception {
    public RiskTypeNotExistException(String message) {
        super(message);
    }

    public RiskTypeNotExistException(Throwable cause) {
        super(cause);
    }

    public RiskTypeNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

}
