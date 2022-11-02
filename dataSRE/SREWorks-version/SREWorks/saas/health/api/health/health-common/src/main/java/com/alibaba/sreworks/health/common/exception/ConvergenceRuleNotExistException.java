package com.alibaba.sreworks.health.common.exception;

/**
 * 异常类型删除异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class ConvergenceRuleNotExistException extends Exception {
    public ConvergenceRuleNotExistException(String message) {
        super(message);
    }

    public ConvergenceRuleNotExistException(Throwable cause) {
        super(cause);
    }

    public ConvergenceRuleNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

}
