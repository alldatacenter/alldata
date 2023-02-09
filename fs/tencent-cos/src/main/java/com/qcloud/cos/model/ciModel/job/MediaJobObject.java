package com.qcloud.cos.model.ciModel.job;

import com.qcloud.cos.internal.CIServiceRequest;
import com.qcloud.cos.model.ciModel.common.MediaInputObject;

import java.io.Serializable;

/**
 * 媒体处理 任务实体 https://cloud.tencent.com/document/product/460/48234
 */

public class MediaJobObject extends CIServiceRequest implements Serializable {
    private String code;
    private String message;
    private String jobId;
    private String state;
    private String creationTime;
    private String endTime;
    private String queueId;
    private String tag;
    private MediaInputObject input = new MediaInputObject();
    private MediaJobOperation operation = new MediaJobOperation();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public MediaInputObject getInput() {
        if (input==null){
            input = new MediaInputObject();
        }
        return input;
    }

    public void setInput(MediaInputObject input) {
        this.input = input;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public MediaJobOperation getOperation() {
        if (operation==null){
            operation = new MediaJobOperation();
        }
        return operation;
    }

    public void setOperation(MediaJobOperation operation) {
        this.operation = operation;
    }

    @Override
    public String toString() {
        return "MediaJobObject{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", jobId='" + jobId + '\'' +
                ", state='" + state + '\'' +
                ", creationTime='" + creationTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", queueId='" + queueId + '\'' +
                ", tag='" + tag + '\'' +
                ", input=" + input +
                ", operation=" + operation +
                '}';
    }
}
