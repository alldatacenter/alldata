package com.qcloud.cos.model.ciModel.workflow;

public class MediaWorkflowStart {
    private String type;
    private MediaWorkflowInput input;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public MediaWorkflowInput getInput() {
        if (input==null){
            input = new MediaWorkflowInput();
        }
        return input;
    }

    public void setInput(MediaWorkflowInput input) {
        this.input = input;
    }

    @Override
    public String toString() {
        return "MediaWorkflowStart{" +
                "type='" + type + '\'' +
                ", input=" + input +
                '}';
    }
}

