package com.qcloud.cos.model.ciModel.job;

/**
 * 媒体处理 任务转码实体 https://cloud.tencent.com/document/product/460/48234
 */
public class MediaTranscodeObject extends MediaVideoCommon {
    /**
     * 容器格式	例:mp4
     */
    private MediaContainerObject container;
    /**
     * 视频信息	不传 Video，相当于删除视频信息
     */
    private MediaTranscodeVideoObject video;
    /**
     * 音频信息
     */
    private MediaAudioObject audio;
    /**
     * 转码配置
     */
    private MediaTransConfigObject transConfig;
    /**
     * 时间区间
     */
    private MediaTimeIntervalObject timeInterval;

    public MediaContainerObject getContainer() {
        if (container == null) {
            container = new MediaContainerObject();
        }
        return container;
    }

    public void setContainer(MediaContainerObject container) {
        this.container = container;
    }

    public MediaTranscodeVideoObject getVideo() {
        if (video == null) {
            video = new MediaTranscodeVideoObject();
        }
        return video;
    }

    public void setVideo(MediaTranscodeVideoObject video) {
        this.video = video;
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

    public MediaTimeIntervalObject getTimeInterval() {
        if (timeInterval == null) {
            timeInterval = new MediaTimeIntervalObject();
        }
        return timeInterval;
    }

    public void setTimeInterval(MediaTimeIntervalObject timeInterval) {
        this.timeInterval = timeInterval;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("MediaTranscodeObject{");
        sb.append("container=").append(container);
        sb.append(", video=").append(video);
        sb.append(", audio=").append(audio);
        sb.append(", transConfig=").append(transConfig);
        sb.append(", timeInterval=").append(timeInterval);
        sb.append('}');
        return sb.toString();
    }
}
