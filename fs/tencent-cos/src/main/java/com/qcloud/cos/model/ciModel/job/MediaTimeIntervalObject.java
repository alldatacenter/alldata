package com.qcloud.cos.model.ciModel.job;


/**
 * 媒体处理 任务时间参数实体 https://cloud.tencent.com/document/product/460/48234
 */
public class MediaTimeIntervalObject {
    /**
     * 开始时间
     */
    private String start;
    /**
     * 持续时间
     */
    private String duration;

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "MediaTimeIntervalObject{" +
                "start='" + start + '\'' +
                ", duration='" + duration + '\'' +
                '}';
    }
}
