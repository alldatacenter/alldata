package com.qcloud.cos.model.ciModel.template;

import com.qcloud.cos.model.ciModel.common.MediaCommonResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * @descript 媒体模板响应实体类。 注释详情请参见 https://cloud.tencent.com/document/product/460/46989
 */
public class MediaListTemplateResponse extends MediaCommonResponse {

    private List<MediaTemplateObject> templateList;
    private String templateId;


    public List<MediaTemplateObject> getTemplateList() {
        if (templateList == null) {
            templateList = new ArrayList<>();
        }
        return templateList;
    }

    public void setTemplateList(List<MediaTemplateObject> templateList) {
        this.templateList = templateList;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    @Override
    public String toString() {
        return "MediaListTemplateResponse{" +
                "templateList=" + templateList +
                ", templateId=" + templateId +
                '}';
    }
}
