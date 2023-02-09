package com.qcloud.cos.model.ciModel.job;

import com.qcloud.cos.model.ciModel.common.MediaInputObject;

/**
 * 响应任务详情实体
 */
public class DocJobDetail {
    /**
     * 任务状态
     */
    private String code;
    /**
     * 任务创建时间
     */
    private String creationTime;
    /**
     * 任务结束时间
     */
    private String endTime;
    /**
     * 源文件位置
     */
    private MediaInputObject input;
    /**
     * 任务唯一id
     */
    private String jobId;
    /**
     * 错误描述，只有 State 为 Failed 时有意义
     */
    private String message;
    /**
     * 队列id
     */
    private String queueId;
    /**
     * 当前任务状态
     */
    private String state;
    /**
     * 任务类型 固定为 DocProcess
     */
    private String tag;
    /**
     * 桶名称
     */
    private String bucketName;
    /**
     * 任务参数实体
     */
    private DocOperationObject operation;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public MediaInputObject getInput() {
        if (input == null) {
            input = new MediaInputObject();
        }
        return input;
    }

    public void setInput(MediaInputObject input) {
        this.input = input;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public DocOperationObject getOperation() {
        if (operation == null) {
            operation = new DocOperationObject();
        }
        return operation;
    }

    public void setOperation(DocOperationObject operation) {
        this.operation = operation;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DocJobDetail{");
        sb.append("code='").append(code).append('\'');
        sb.append(", creationTime='").append(creationTime).append('\'');
        sb.append(", endTime='").append(endTime).append('\'');
        sb.append(", input=").append(input);
        sb.append(", jobId='").append(jobId).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", queueId='").append(queueId).append('\'');
        sb.append(", state='").append(state).append('\'');
        sb.append(", tag='").append(tag).append('\'');
        sb.append(", bucketName='").append(bucketName).append('\'');
        sb.append(", operation=").append(operation);
        sb.append('}');
        return sb.toString();
    }
}
