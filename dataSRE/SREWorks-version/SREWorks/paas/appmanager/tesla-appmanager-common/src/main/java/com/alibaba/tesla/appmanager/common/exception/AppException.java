package com.alibaba.tesla.appmanager.common.exception;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * 全局异常
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class AppException extends RuntimeException {

    protected AppErrorCode errorCode;
    protected String errorMessage;
    protected Object errorData;

    public AppException() {
        this.errorCode = AppErrorCode.UNKNOWN_ERROR;
        this.errorMessage = null;
        this.errorData = null;
    }

    public AppException(AppErrorCode errorCode) {
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getDescription();
        this.errorData = null;
    }

    public AppException(String errorMessage) {
        super(errorMessage);
        this.errorCode = AppErrorCode.UNKNOWN_ERROR;
        this.errorMessage = errorMessage;
        this.errorData = null;
    }

    public AppException(Throwable cause) {
        super(cause);
        this.errorCode = AppErrorCode.UNKNOWN_ERROR;
        this.errorMessage = cause.toString();
        this.errorData = ExceptionUtils.getStackFrames(cause);
    }

    public AppException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = AppErrorCode.UNKNOWN_ERROR;
        this.errorMessage = errorMessage;
        this.errorData = ExceptionUtils.getStackFrames(cause);
    }

    public AppException(AppErrorCode errorCode, String errorMessage, Object errorData) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorData = errorData;
    }

    public AppException(AppErrorCode errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorData = null;
    }

    public AppException(AppErrorCode errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorData = null;
    }

    public AppException(AppErrorCode errorCode, String errorMessage, Object errorData, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorData = errorData;
    }

    public AppErrorCode getErrorCode() {
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
        if (this.getCause() != null) {
            exceptionStr += ". Cause: " + ExceptionUtils.getStackTrace(this.getCause());
        }
        if (getErrorData() != null) {
            exceptionStr += ". Details: " + ToStringBuilder.reflectionToString(getErrorData());
        }
        return exceptionStr;
    }
}
