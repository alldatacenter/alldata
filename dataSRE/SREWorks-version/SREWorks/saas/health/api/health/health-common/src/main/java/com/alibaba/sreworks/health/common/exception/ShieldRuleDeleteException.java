package com.alibaba.sreworks.health.common.exception;

/**
 * 异常类型删除异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class ShieldRuleDeleteException extends Exception {
    public ShieldRuleDeleteException(String message) {
        super(message);
    }

    public ShieldRuleDeleteException(Throwable cause) {
        super(cause);
    }

    public ShieldRuleDeleteException(String message, Throwable cause) {
        super(message, cause);
    }

}
