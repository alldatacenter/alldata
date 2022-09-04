package com.alibaba.tesla.authproxy.api.exception;

public class AuthProxyUnauthorizedException extends AuthProxyClientException {

    public AuthProxyUnauthorizedException() {
    }

    public AuthProxyUnauthorizedException(String message) {
        super(message);
    }

    public AuthProxyUnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthProxyUnauthorizedException(Throwable cause) {
        super(cause);
    }

    public AuthProxyUnauthorizedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
