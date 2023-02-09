package com.qcloud.cos.model.ciModel.workflow;

import com.qcloud.cos.model.ciModel.common.MediaOutputObject;

public class MediaOperation {
    private String templateId;
    private MediaOutputObject output;

    public MediaOutputObject getOutput() {
        if (output == null){
            output = new MediaOutputObject();
        }
        return output;
    }

    public void setOutput(MediaOutputObject output) {
        this.output = output;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    @Override
    public String toString() {
        return "MediaOperation{" +
                "templateId='" + templateId + '\'' +
                ", output=" + output +
                '}';
    }
}
