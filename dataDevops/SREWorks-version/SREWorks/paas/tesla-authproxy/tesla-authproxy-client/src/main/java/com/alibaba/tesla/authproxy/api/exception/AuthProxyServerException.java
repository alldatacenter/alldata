package com.alibaba.tesla.authproxy.api.exception;

public class AuthProxyServerException extends AuthProxyException {
    public AuthProxyServerException() {
    }

    public AuthProxyServerException(String message) {
        super(message);
    }

    public AuthProxyServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthProxyServerException(Throwable cause) {
        super(cause);
    }

    public AuthProxyServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
