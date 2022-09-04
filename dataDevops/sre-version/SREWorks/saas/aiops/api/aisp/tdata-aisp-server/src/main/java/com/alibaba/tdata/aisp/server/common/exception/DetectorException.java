package com.alibaba.tdata.aisp.server.common.exception;

import com.alibaba.fastjson.JSONObject;

import lombok.Data;

/**
 * @ClassName: DetectorException
 * @Author: dyj
 * @DATE: 2021-11-25
 * @Description:
 **/
@Data
public class DetectorException extends RuntimeException{
    protected int httpStatus;
    protected String taskUUID;
    protected String code;
    protected String message;

    public DetectorException(int httpStatus, JSONObject body){
        this.httpStatus = httpStatus;
        this.taskUUID = body.getString("taskUUID");
        this.code = body.getString("code");
        this.message = body.getString("message");
    }

    public DetectorException(int httpStatus, String taskUUID, String code, String message){
        this.httpStatus = httpStatus;
        this.taskUUID = taskUUID;
        this.code = code;
        this.message = message;
    }

    public DetectorException(int httpStatus, String taskUUID, String code, String message, Throwable e){
        this.httpStatus = httpStatus;
        this.taskUUID = taskUUID;
        this.code = code;
        this.message = message;
    }

    public DetectorException(int httpStatus, String code, String message){
        this.httpStatus = httpStatus;
        this.taskUUID = null;
        this.code = code;
        this.message = message;
    }

    public DetectorException(int httpStatus, String code, String message, Throwable e){
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
