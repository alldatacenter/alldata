package com.qcloud.cos.model.ciModel.workflow;

public class MediaWorkflowNode {
    private String type;
    private MediaOperation operation;
    private MediaWorkflowInput input;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public MediaOperation getOperation() {
        if (operation == null) {
            operation = new MediaOperation();
        }
        return operation;
    }

    public void setOperation(MediaOperation operation) {

        this.operation = operation;
    }

    public MediaWorkflowInput getInput() {
        if (input == null) {
            input = new MediaWorkflowInput();
        }
        return input;
    }

    public void setInput(MediaWorkflowInput input) {
        this.input = input;
    }

    @Override
    public String toString() {
        return "MediaWorkflowNode{" +
                "type='" + type + '\'' +
                ", operation=" + operation +
                ", input=" + input +
                '}';
    }
}
