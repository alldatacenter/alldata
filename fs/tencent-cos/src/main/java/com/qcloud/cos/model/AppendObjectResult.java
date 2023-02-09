package com.qcloud.cos.model;

public class AppendObjectResult {
    private ObjectMetadata metadata;
    private Long nextAppendPosition;

    public ObjectMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ObjectMetadata metadata) {
        this.metadata = metadata;
    }

    public Long getNextAppendPosition() {
        return nextAppendPosition;
    }

    public void setNextAppendPosition(Long nextAppendPosition) {
        this.nextAppendPosition = nextAppendPosition;
    }
}
