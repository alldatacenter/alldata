package com.alibaba.tesla.authproxy.api.exception;

public class AuthProxyException extends Exception {

    public AuthProxyException() {
    }

    public AuthProxyException(String message) {
        super(message);
    }

    public AuthProxyException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthProxyException(Throwable cause) {
        super(cause);
    }

    public AuthProxyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
