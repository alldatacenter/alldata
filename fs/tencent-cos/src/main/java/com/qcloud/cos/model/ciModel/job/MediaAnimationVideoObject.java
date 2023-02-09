package com.qcloud.cos.model.ciModel.job;

/**
 * 媒体处理 动图任务 video实体 https://cloud.tencent.com/document/product/460/48217
 */
public class MediaAnimationVideoObject extends MediaVideoCommon {

    /**
     * 动图只保留关键帧
     */
    private String animateOnlyKeepKeyFrame;
    /**
     * 动图抽帧间隔时间
     */
    private String animateTimeIntervalOfFrame;
    /**
     * Animation 每秒抽帧帧数
     */
    private String animateFramesPerSecond;
    /**
     * 设置相对质量
     */
    private String qality;

    public String getAnimateOnlyKeepKeyFrame() {
        return animateOnlyKeepKeyFrame;
    }

    public void setAnimateOnlyKeepKeyFrame(String animateOnlyKeepKeyFrame) {
        this.animateOnlyKeepKeyFrame = animateOnlyKeepKeyFrame;
    }

    public String getAnimateTimeIntervalOfFrame() {
        return animateTimeIntervalOfFrame;
    }

    public void setAnimateTimeIntervalOfFrame(String animateTimeIntervalOfFrame) {
        this.animateTimeIntervalOfFrame = animateTimeIntervalOfFrame;
    }

    public String getAnimateFramesPerSecond() {
        return animateFramesPerSecond;
    }

    public void setAnimateFramesPerSecond(String animateFramesPerSecond) {
        this.animateFramesPerSecond = animateFramesPerSecond;
    }

    public String getQality() {
        return qality;
    }

    public void setQality(String qality) {
        this.qality = qality;
    }

    @Override
    public String toString() {
        return "MediaAnimationVideoObject{" +
                "animateOnlyKeepKeyFrame='" + animateOnlyKeepKeyFrame + '\'' +
                ", animateTimeIntervalOfFrame='" + animateTimeIntervalOfFrame + '\'' +
                ", animateFramesPerSecond='" + animateFramesPerSecond + '\'' +
                ", qality='" + qality + '\'' +
                '}';
    }
}
