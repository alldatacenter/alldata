package com.qcloud.cos.model.ciModel.workflow;

public class MediaWorkflowNodes {
    private MediaWorkflowStart mediaWorkflowStart;
    private MediaWorkflowNode node;
    private MediaOperation operation;
    private String Type;

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public MediaOperation getOperation() {
        return operation;
    }

    public void setOperation(MediaOperation operation) {
        this.operation = operation;
    }

    public MediaWorkflowStart getMediaWorkflowStart() {
        return mediaWorkflowStart;
    }

    public void setMediaWorkflowStart(MediaWorkflowStart mediaWorkflowStart) {
        this.mediaWorkflowStart = mediaWorkflowStart;
    }

    @Override
    public String toString() {
        return "MediaWorkflowNodes{" +
                "mediaWorkflowStart=" + mediaWorkflowStart +
                ", node=" + node +
                ", operation=" + operation +
                '}';
    }
}
