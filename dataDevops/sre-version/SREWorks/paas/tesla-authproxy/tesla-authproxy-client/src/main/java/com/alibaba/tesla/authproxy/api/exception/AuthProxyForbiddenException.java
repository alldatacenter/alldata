package com.alibaba.tesla.authproxy.api.exception;

public class AuthProxyForbiddenException extends AuthProxyClientException {
    public AuthProxyForbiddenException() {
    }

    public AuthProxyForbiddenException(String message) {
        super(message);
    }

    public AuthProxyForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthProxyForbiddenException(Throwable cause) {
        super(cause);
    }

    public AuthProxyForbiddenException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
