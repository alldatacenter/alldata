package com.alibaba.sreworks.health.common.exception;

/**
 * 异常类型删除异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class ConvergenceRuleExistException extends Exception {
    public ConvergenceRuleExistException(String message) {
        super(message);
    }

    public ConvergenceRuleExistException(Throwable cause) {
        super(cause);
    }

    public ConvergenceRuleExistException(String message, Throwable cause) {
        super(message, cause);
    }

}
