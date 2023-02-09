package com.qcloud.cos.model.ciModel.template;

import com.qcloud.cos.model.ciModel.common.MediaCommonResponse;

/**
 * @descript 媒体模板响应实体类。 注释详情请参见 https://cloud.tencent.com/document/product/460/46989
 */
public class MediaTemplateResponse extends MediaCommonResponse {

    private MediaTemplateObject template;
    private MediaTemplateObject templateId;

    public MediaTemplateObject getTemplate() {
        if (template == null){
            template = new MediaTemplateObject();
        }
        return template;
    }

    public void setTemplate(MediaTemplateObject template) {
        this.template = template;
    }

    public MediaTemplateObject getTemplateId() {
        return templateId;
    }

    public void setTemplateId(MediaTemplateObject templateId) {
        this.templateId = templateId;
    }

    @Override
    public String toString() {
        return "MediaTemplateResponse{" +
                "template=" + template +
                '}';
    }
}
