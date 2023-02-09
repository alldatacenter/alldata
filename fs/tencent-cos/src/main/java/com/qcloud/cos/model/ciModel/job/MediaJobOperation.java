package com.qcloud.cos.model.ciModel.job;

import com.qcloud.cos.model.ciModel.common.MediaOutputObject;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoObjcet;
import com.qcloud.cos.model.ciModel.template.MediaWatermark;

import java.util.ArrayList;
import java.util.List;

/**
 * 媒体处理 operation实体 https://cloud.tencent.com/document/product/460/48234
 */

public class MediaJobOperation {
    private String templateId;
    private MediaOutputObject output;
    private MediaAnimationObject mediaAnimation;
    private MediaInfoObjcet mediaInfo;
    private MediaRemoveWaterMark removeWatermark;
    private MediaWatermark watermark;
    private MediaTranscodeObject transcode;
    private List<String> watermarkTemplateId;
    private MediaConcatTemplateObject mediaConcatTemplate;

    public MediaJobOperation() {
        this.output = new MediaOutputObject();
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public MediaOutputObject getOutput() {
        if (output == null) {
            output = new MediaOutputObject();
        }
        return output;
    }

    public void setOutput(MediaOutputObject output) {
        this.output = output;
    }

    public MediaAnimationObject getMediaAnimation() {
        if (mediaAnimation == null) {
            mediaAnimation = new MediaAnimationObject();
        }
        return mediaAnimation;
    }

    public MediaInfoObjcet getMediaInfo() {
        if (mediaInfo == null) {
            mediaInfo = new MediaInfoObjcet();
        }
        return mediaInfo;
    }

    public void setMediaInfo(MediaInfoObjcet mediaInfo) {
        this.mediaInfo = mediaInfo;
    }

    public MediaRemoveWaterMark getRemoveWatermark() {
        if (removeWatermark == null) {
            removeWatermark = new MediaRemoveWaterMark();
        }
        return removeWatermark;
    }

    public void setRemoveWatermark(MediaRemoveWaterMark removeWatermark) {
        this.removeWatermark = removeWatermark;
    }

    public void setMediaAnimation(MediaAnimationObject mediaAnimation) {
        this.mediaAnimation = mediaAnimation;
    }

    public MediaWatermark getWatermark() {
        if (watermark == null) {
            watermark = new MediaWatermark();
        }
        return watermark;
    }

    public void setWatermark(MediaWatermark watermark) {
        this.watermark = watermark;
    }


    public List<String> getWatermarkTemplateId() {
        if (watermarkTemplateId == null) {
            watermarkTemplateId = new ArrayList<>();
        }
        return watermarkTemplateId;
    }

    public void setWatermarkTemplateId(List<String> watermarkTemplateId) {
        this.watermarkTemplateId = watermarkTemplateId;
    }

    public MediaConcatTemplateObject getMediaConcatTemplate() {
        if (mediaConcatTemplate == null) {
            mediaConcatTemplate = new MediaConcatTemplateObject();
        }
        return mediaConcatTemplate;
    }

    public void setMediaConcatTemplate(MediaConcatTemplateObject mediaConcatTemplate) {
        this.mediaConcatTemplate = mediaConcatTemplate;
    }

    public MediaTranscodeObject getTranscode() {
        if (transcode == null) {
            transcode = new MediaTranscodeObject();
        }
        return transcode;
    }

    public void setTranscode(MediaTranscodeObject transcode) {
        this.transcode = transcode;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("MediaJobOperation{");
        sb.append("templateId='").append(templateId).append('\'');
        sb.append(", output=").append(output);
        sb.append(", mediaAnimation=").append(mediaAnimation);
        sb.append(", mediaInfo=").append(mediaInfo);
        sb.append(", removeWatermark=").append(removeWatermark);
        sb.append(", watermark=").append(watermark);
        sb.append(", transcode=").append(transcode);
        sb.append(", watermarkTemplateId=").append(watermarkTemplateId);
        sb.append(", mediaConcatTemplate=").append(mediaConcatTemplate);
        sb.append('}');
        return sb.toString();
    }
}
