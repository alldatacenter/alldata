package com.alibaba.tesla.authproxy.lib.exceptions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * 权代异常基类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class AuthProxyException extends RuntimeException {

    private static Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    protected AuthProxyErrorCode errorCode;
    protected String errorMessage;
    protected Object errorData;

    public AuthProxyException() {
        this.errorCode = AuthProxyErrorCode.UNKNOWN_ERROR;
        this.errorMessage = null;
        this.errorData = null;
    }

    public AuthProxyException(String errorMessage) {
        super(errorMessage);
        this.errorCode = AuthProxyErrorCode.UNKNOWN_ERROR;
        this.errorMessage = errorMessage;
        this.errorData = null;
    }

    public AuthProxyException(Throwable cause) {
        super(cause);
        this.errorCode = AuthProxyErrorCode.UNKNOWN_ERROR;
        this.errorMessage = cause.toString();
        this.errorData = ExceptionUtils.getStackFrames(cause);
    }

    public AuthProxyException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = AuthProxyErrorCode.UNKNOWN_ERROR;
        this.errorMessage = errorMessage;
        this.errorData = ExceptionUtils.getStackFrames(cause);
    }

    public AuthProxyException(AuthProxyErrorCode errorCode, String errorMessage, Object errorData) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorData = errorData;
    }

    public AuthProxyException(AuthProxyErrorCode errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorData = null;
    }

    public AuthProxyException(AuthProxyErrorCode errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorData = null;
    }

    public AuthProxyException(AuthProxyErrorCode errorCode, String errorMessage, Object errorData, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorData = errorData;
    }

    public AuthProxyErrorCode getErrorCode() {
        return this.errorCode;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public Object getErrorData() {
        return this.errorData;
    }

    @Override
    public String toString() {
        int code = getErrorCode().getCode();
        String message = getErrorMessage();
        if (message == null || message.length() == 0) {
            message = getErrorCode().getDescription();
        }
        String exceptionStr = String.format("[ERR-%s] %s", code, message);
        if (getErrorData() != null) {
            exceptionStr += ". Details: " + gson.toJson(getErrorData());
        }
        return exceptionStr;
    }
}
