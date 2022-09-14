package com.alibaba.sreworks.health.common.exception;

/**
 * 异常类型删除异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class ShieldRuleNotExistException extends Exception {
    public ShieldRuleNotExistException(String message) {
        super(message);
    }

    public ShieldRuleNotExistException(Throwable cause) {
        super(cause);
    }

    public ShieldRuleNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

}
