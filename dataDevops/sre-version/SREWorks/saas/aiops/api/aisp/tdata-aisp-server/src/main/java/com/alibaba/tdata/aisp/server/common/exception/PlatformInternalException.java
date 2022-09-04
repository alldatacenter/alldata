package com.alibaba.tdata.aisp.server.common.exception;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;

/**
 * @ClassName: ServiceException
 * @Author: dyj
 * @DATE: 2021-03-29
 * @Description:
 **/
@Data
public class PlatformInternalException extends RuntimeException{
    protected String taskUUID;
    protected int httpStatus;
    protected String message;
    protected Object data;

    public PlatformInternalException(){
        this.message = null;
        this.data = null;
    }

    public PlatformInternalException(String errorMessage){
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();
        this.taskUUID = null;
        this.message = errorMessage;
        this.data = null;
    }

    public PlatformInternalException(String errorMessage, Throwable cause){
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();
        this.taskUUID = null;
        this.message = errorMessage;
        this.data = ExceptionUtils.getStackFrames(cause);
    }

    public PlatformInternalException(int httpStatus, String taskUUID, String errorMessage, Throwable cause){
        this.httpStatus = httpStatus;
        this.taskUUID = taskUUID;
        this.message = errorMessage;
        this.data = ExceptionUtils.getStackFrames(cause);
    }

    public PlatformInternalException(String taskUUID, String errorMessage, Throwable cause){
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();
        this.taskUUID = taskUUID;
        this.message = errorMessage;
        this.data = ExceptionUtils.getStackFrames(cause);
    }

    @Override
    public String getMessage(){
        return this.message;
    }

    public Object getData(){
        return this.data;
    }

    @Override
    public String toString() {
        String exceptionStr = getMessage();
        if (this.getCause() != null) {
            exceptionStr += "/n [Cause]: " + ExceptionUtils.getStackTrace(this.getCause());
        }
        if (getData() != null) {
            exceptionStr += "/n Details: " + ToStringBuilder.reflectionToString(getData());
        }
        return exceptionStr;
    }
}
