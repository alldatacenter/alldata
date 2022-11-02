package com.elasticsearch.cloud.monitor.metric.common.rule.exception;

/**
 * @author xiaoping
 * @date 2019/11/25
 */
public class InvalidParameterException extends Exception {
    public InvalidParameterException() {
    }

    public InvalidParameterException(String message) {
        super(message);
    }

    public InvalidParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidParameterException(Throwable cause) {
        super(cause);
    }
}
