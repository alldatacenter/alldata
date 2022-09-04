package com.alibaba.tesla.gateway.common.exception;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class OperatorRouteException extends Exception{
    public OperatorRouteException(String message) {
        super(message);
    }

    public OperatorRouteException(String message, Throwable cause) {
        super(message, cause);
    }

    public OperatorRouteException(Throwable cause) {
        super(cause);
    }
}
