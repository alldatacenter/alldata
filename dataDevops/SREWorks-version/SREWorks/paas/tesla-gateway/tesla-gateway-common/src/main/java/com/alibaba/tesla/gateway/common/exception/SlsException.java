package com.alibaba.tesla.gateway.common.exception;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class SlsException extends Exception {
    public SlsException(String message) {
        super(message);
    }

    public SlsException(Throwable cause) {
        super(cause);
    }
}
