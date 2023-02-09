package com.qcloud.cos.model.ciModel.job;

/**
 * 媒体处理 动图任务实体 https://cloud.tencent.com/document/product/460/48217
 */
public class MediaAnimationObject {
    private MediaContainerObject container;
    private MediaVideoObject video;
    private MediaTimeIntervalObject timeInterval;

    public MediaContainerObject getContainer() {
        if (container==null){
            container= new MediaContainerObject();
        }
        return container;
    }

    public void setContainer(MediaContainerObject container) {
        this.container = container;
    }

    public MediaVideoObject getVideo() {
        if (video==null){
            video= new MediaVideoObject();
        }
        return video;
    }

    public void setVideo(MediaVideoObject video) {
        this.video = video;
    }

    public MediaTimeIntervalObject getTimeInterval() {
        if (timeInterval==null){
            timeInterval= new MediaTimeIntervalObject();
        }
        return timeInterval;
    }

    public void setTimeInterval(MediaTimeIntervalObject timeInterval) {
        this.timeInterval = timeInterval;
    }

    @Override
    public String toString() {
        return "MediaAnimationObject{" +
                "container=" + container +
                ", video=" + video +
                ", timeInterval=" + timeInterval +
                '}';
    }
}
