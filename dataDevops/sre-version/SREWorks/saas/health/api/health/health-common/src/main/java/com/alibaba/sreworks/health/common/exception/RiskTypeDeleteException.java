package com.alibaba.sreworks.health.common.exception;

/**
 * 风险类型删除异常
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:42
 */
public class RiskTypeDeleteException extends Exception {
    public RiskTypeDeleteException(String message) {
        super(message);
    }

    public RiskTypeDeleteException(Throwable cause) {
        super(cause);
    }

    public RiskTypeDeleteException(String message, Throwable cause) {
        super(message, cause);
    }

}
