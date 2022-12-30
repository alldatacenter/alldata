package com.alibaba.tesla.authproxy.api.exception;

public class AuthProxyClientException extends AuthProxyException {
    public AuthProxyClientException() {
    }

    public AuthProxyClientException(String message) {
        super(message);
    }

    public AuthProxyClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthProxyClientException(Throwable cause) {
        super(cause);
    }

    public AuthProxyClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
