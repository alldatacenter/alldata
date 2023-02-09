package com.qcloud.cos.model.ciModel.template;

import com.qcloud.cos.model.ciModel.job.*;

/**
 * @descript 媒体模板响应实体类。 注释详情请参见 https://cloud.tencent.com/document/product/460/46989
 */
public class MediaTemplateTransTplObject  {
    /**
     * 容器格式
     */
    private MediaContainerObject container = new MediaContainerObject();
    /**
     * 视频信息
     */
    private MediaVideoObject video = new MediaVideoObject();
    /**
     * 时间区间
     */
    private MediaTimeIntervalObject timeInterval = new MediaTimeIntervalObject();

    /**
     * 时间区间
     */
    private MediaAudioObject audio = new MediaAudioObject();

    /**
     * 截图
     */
    private MediaSnapshotObject snapshot;

    private MediaTransConfigObject transConfig;

    public MediaContainerObject getContainer() {
        if (container==null){
            container = new MediaContainerObject();
        }
        return container;
    }

    public void setContainer(MediaContainerObject container) {
        this.container = container;
    }

    public MediaVideoObject getVideo() {
        if (video==null){
            video = new MediaVideoObject();
        }
        return video;
    }

    public void setVideo(MediaVideoObject video) {
        this.video = video;
    }

    public MediaTimeIntervalObject getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(MediaTimeIntervalObject timeInterval) {
        this.timeInterval = timeInterval;
    }

    public MediaSnapshotObject getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(MediaSnapshotObject snapshot) {
        this.snapshot = snapshot;
    }

    public MediaAudioObject getAudio() {
        if (audio==null){
            audio = new MediaAudioObject();
        }
        return audio;
    }

    public void setAudio(MediaAudioObject audio) {
        this.audio = audio;
    }

    public MediaTransConfigObject getTransConfig() {
        if (transConfig==null){
            transConfig = new MediaTransConfigObject();
        }
        return transConfig;
    }

    public void setTransConfig(MediaTransConfigObject transConfig) {
        this.transConfig = transConfig;
    }

    @Override
    public String toString() {
        return "MediaTemplateTransTplObject{" +
                "container=" + container +
                ", video=" + video +
                ", timeInterval=" + timeInterval +
                ", audio=" + audio +
                ", snapshot=" + snapshot +
                ", transConfig=" + transConfig +
                '}';
    }
}
