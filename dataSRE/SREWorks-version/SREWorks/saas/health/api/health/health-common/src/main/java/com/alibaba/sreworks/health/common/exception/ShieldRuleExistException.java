package com.alibaba.sreworks.health.common.exception;

/**
 * 异常类型删除异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class ShieldRuleExistException extends Exception {
    public ShieldRuleExistException(String message) {
        super(message);
    }

    public ShieldRuleExistException(Throwable cause) {
        super(cause);
    }

    public ShieldRuleExistException(String message, Throwable cause) {
        super(message, cause);
    }

}
