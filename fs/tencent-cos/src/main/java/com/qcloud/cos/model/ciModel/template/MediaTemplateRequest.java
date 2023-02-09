package com.qcloud.cos.model.ciModel.template;

import com.qcloud.cos.internal.CIServiceRequest;
import com.qcloud.cos.model.ciModel.job.*;

import java.io.Serializable;

/**
 * @descript 本类为模板实体类。 注释详情请参见 https://cloud.tencent.com/document/product/460/46989
 */
public class MediaTemplateRequest extends CIServiceRequest implements Serializable {
    /**
     * 模板id
     */
    private String templateId;
    /**
     * 动图模板 : Animation 截图模板Snapshot
     * 水印模板 : Watermark 截图模板Transcode
     */
    private String tag;
    /**
     * 模板名称 仅支持中文、英文、数字、_、-和*
     */
    private String name;
    /**
     * 容器格式
     */
    private MediaContainerObject container;
    /**
     * 视频信息
     */
    private MediaVideoObject video;
    /**
     * 音频信息
     */
    private MediaAudioObject audio;
    /**
     * 视频信息
     */
    private MediaTransConfigObject transConfig;
    /**
     * 时间区间
     */
    private MediaTimeIntervalObject timeInterval;
    /**
     * 截图
     */
    private MediaSnapshotObject snapshot;
    /**
     * 水印
     */
    private MediaWatermark watermark;
    /**
     * 第几页
     */
    private String pageNumber;
    /**
     * 每页个数
     */
    private String pageSize;
    /**
     * Official，Custom，默认值：Custom
     */
    private String category;
    /**
     * 模板 ID，以,符号分割字符串
     */
    private String ids;

    public String getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(String pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MediaContainerObject getContainer() {
        if (container == null)
            container = new MediaContainerObject();
        return container;
    }

    public void setContainer(MediaContainerObject container) {
        this.container = container;
    }

    public MediaTimeIntervalObject getTimeInterval() {
        if (timeInterval == null)
            timeInterval = new MediaTimeIntervalObject();
        return timeInterval;
    }

    public void setTimeInterval(MediaTimeIntervalObject timeInterval) {
        this.timeInterval = timeInterval;
    }

    public MediaVideoObject getVideo() {
        if (video == null)
            video = new MediaVideoObject();
        return video;
    }

    public void setVideo(MediaVideoObject video) {
        this.video = video;
    }

    public MediaSnapshotObject getSnapshot() {
        if (snapshot == null)
            snapshot = new MediaSnapshotObject();
        return snapshot;
    }

    public void setSnapshot(MediaSnapshotObject snapshot) {
        this.snapshot = snapshot;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIds() {
        return ids;
    }

    public void setIds(String ids) {
        this.ids = ids;
    }

    public MediaAudioObject getAudio() {
        if (audio == null) {
            audio = new MediaAudioObject();
        }
        return audio;
    }

    public void setAudio(MediaAudioObject audio) {
        this.audio = audio;
    }

    public MediaTransConfigObject getTransConfig() {
        if (transConfig == null) {
            transConfig = new MediaTransConfigObject();
        }
        return transConfig;
    }

    public void setTransConfig(MediaTransConfigObject transConfig) {
        this.transConfig = transConfig;
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

    @Override
    public String toString() {
        return "MediaTemplateRequest{" +
                "templateId='" + templateId + '\'' +
                ", tag='" + tag + '\'' +
                ", name='" + name + '\'' +
                ", container=" + container +
                ", video=" + video +
                ", audio=" + audio +
                ", transConfig=" + transConfig +
                ", timeInterval=" + timeInterval +
                ", snapshot=" + snapshot +
                ", waterMark=" + watermark +
                ", pageNumber='" + pageNumber + '\'' +
                ", pageSize='" + pageSize + '\'' +
                ", category='" + category + '\'' +
                ", ids='" + ids + '\'' +
                '}';
    }
}
