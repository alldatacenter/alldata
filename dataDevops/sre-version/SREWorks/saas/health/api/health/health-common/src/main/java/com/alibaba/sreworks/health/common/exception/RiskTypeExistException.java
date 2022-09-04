package com.alibaba.sreworks.health.common.exception;

/**
 * 风险类型存在
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class RiskTypeExistException extends Exception {
    public RiskTypeExistException(String message) {
        super(message);
    }

    public RiskTypeExistException(Throwable cause) {
        super(cause);
    }

    public RiskTypeExistException(String message, Throwable cause) {
        super(message, cause);
    }

}
