package com.qcloud.cos.model.ciModel.workflow;

public class MediaWorkflowDependency {
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "MediaWorkflowDependency{" +
                "value='" + value + '\'' +
                '}';
    }
}
