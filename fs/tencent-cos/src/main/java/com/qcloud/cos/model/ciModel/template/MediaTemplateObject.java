package com.qcloud.cos.model.ciModel.template;

import com.qcloud.cos.model.ciModel.common.MediaCommonResponse;
import com.qcloud.cos.model.ciModel.job.MediaTransConfigObject;

/**
 * @descript 媒体模板响应实体类。 注释详情请参见 https://cloud.tencent.com/document/product/460/46989
 */
public class MediaTemplateObject extends MediaCommonResponse {

    private String templateId;

    private String name;

    private String tag;

    private String state;

    private String bucketId;
    private String category;

    private MediaTemplateTransTplObject transTpl;

    private MediaSnapshotObject snapshot;

    private MediaWatermark watermark;

    private MediaTransConfigObject transConfig;

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String bucketId) {
        this.bucketId = bucketId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public MediaTemplateTransTplObject getTransTpl() {
        if (transTpl==null){
            transTpl = new MediaTemplateTransTplObject();
        }
        return transTpl;
    }

    public void setTransTpl(MediaTemplateTransTplObject transTpl) {
        this.transTpl = transTpl;
    }

    public MediaSnapshotObject getSnapshot() {
        if (snapshot==null){
            snapshot = new MediaSnapshotObject();
        }
        return snapshot;
    }

    public void setSnapshot(MediaSnapshotObject snapshot) {
        this.snapshot = snapshot;
    }

    public MediaWatermark getWatermark() {
        if (watermark==null){
            watermark = new MediaWatermark();
        }
        return watermark;
    }

    public void setWatermark(MediaWatermark watermark) {
        this.watermark = watermark;
    }

    public MediaTransConfigObject getTransConfig() {
        return transConfig;
    }

    public void setTransConfig(MediaTransConfigObject transConfig) {
        this.transConfig = transConfig;
    }

    @Override
    public String toString() {
        return "MediaTemplateObject{" +
                "templateId='" + templateId + '\'' +
                ", name='" + name + '\'' +
                ", tag='" + tag + '\'' +
                ", state='" + state + '\'' +
                ", bucketId='" + bucketId + '\'' +
                ", category='" + category + '\'' +
                ", transTpl=" + transTpl +
                ", snapshot=" + snapshot +
                ", watermark=" + watermark +
                ", transConfig=" + transConfig +
                '}';
    }
}
