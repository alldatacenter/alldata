package com.alibaba.tesla.authproxy.api.exception;

public class AuthProxyParamException extends AuthProxyClientException {

    public AuthProxyParamException() {
    }

    public AuthProxyParamException(String message) {
        super(message);
    }

    public AuthProxyParamException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthProxyParamException(Throwable cause) {
        super(cause);
    }

    public AuthProxyParamException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
