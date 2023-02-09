package com.qcloud.cos.model.ciModel.workflow;

public class MediaWorkflowInput {
    private String queueId;
    private String objectPrefix;

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public String getObjectPrefix() {
        return objectPrefix;
    }

    public void setObjectPrefix(String objectPrefix) {
        this.objectPrefix = objectPrefix;
    }

    @Override
    public String toString() {
        return "MediaWorkflowInput{" +
                "queueId='" + queueId + '\'' +
                ", objectPrefix='" + objectPrefix + '\'' +
                '}';
    }
}
