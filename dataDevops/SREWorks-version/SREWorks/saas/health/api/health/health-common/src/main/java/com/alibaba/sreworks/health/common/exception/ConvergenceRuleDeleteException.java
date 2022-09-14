package com.alibaba.sreworks.health.common.exception;

/**
 * 异常类型删除异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class ConvergenceRuleDeleteException extends Exception {
    public ConvergenceRuleDeleteException(String message) {
        super(message);
    }

    public ConvergenceRuleDeleteException(Throwable cause) {
        super(cause);
    }

    public ConvergenceRuleDeleteException(String message, Throwable cause) {
        super(message, cause);
    }

}
