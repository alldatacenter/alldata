package com.qcloud.cos.model.ciModel.job;

import java.io.Serializable;
/**
 * 媒体处理 任务转码配置实体 https://cloud.tencent.com/document/product/460/48234
 */
public class MediaTransConfigObject implements Serializable {

    /**
     * 分辨率调整方式 取值 scale、crop、pad、none
     * 当输出视频的宽高比与原视频不等时，根据此参数做分辨率的相应调整
     */
    private String adjDarMethod;
    /**
     * 是否检查分辨率 true、false
     * 当为 false时，按照配置参数转码
     */
    private String isCheckReso;
    /**
     * 分辨率调整方式 取值0、1；0 表示使用原视频分辨率；
     * 1表示返回转码失败
     * 当 IsCheckReso 为 true 时生效
     */
    private String resoAdjMethod;
    /**
     * 是否检查视频码率
     */
    private String isCheckVideoBitrate;
    /**
     * 视频码率调整方式
     */
    private String videoBitrateAdjMethod;
    /**
     * 是否检查音频码率
     */
    private String isCheckAudioBitrate;
    /**
     * 音频码率调整方式
     */
    private String audioBitrateAdjMethod;

    private String transMode;

    public String getAdjDarMethod() {
        return adjDarMethod;
    }

    public void setAdjDarMethod(String adjDarMethod) {
        this.adjDarMethod = adjDarMethod;
    }

    public String getIsCheckReso() {
        return isCheckReso;
    }

    public void setIsCheckReso(String isCheckReso) {
        this.isCheckReso = isCheckReso;
    }

    public String getResoAdjMethod() {
        return resoAdjMethod;
    }

    public void setResoAdjMethod(String resoAdjMethod) {
        this.resoAdjMethod = resoAdjMethod;
    }

    public String getIsCheckVideoBitrate() {
        return isCheckVideoBitrate;
    }

    public void setIsCheckVideoBitrate(String isCheckVideoBitrate) {
        this.isCheckVideoBitrate = isCheckVideoBitrate;
    }

    public String getVideoBitrateAdjMethod() {
        return videoBitrateAdjMethod;
    }

    public void setVideoBitrateAdjMethod(String videoBitrateAdjMethod) {
        this.videoBitrateAdjMethod = videoBitrateAdjMethod;
    }

    public String getIsCheckAudioBitrate() {
        return isCheckAudioBitrate;
    }

    public void setIsCheckAudioBitrate(String isCheckAudioBitrate) {
        this.isCheckAudioBitrate = isCheckAudioBitrate;
    }

    public String getAudioBitrateAdjMethod() {
        return audioBitrateAdjMethod;
    }

    public void setAudioBitrateAdjMethod(String audioBitrateAdjMethod) {
        this.audioBitrateAdjMethod = audioBitrateAdjMethod;
    }

    public String getTransMode() {
        return transMode;
    }

    public void setTransMode(String transMode) {
        this.transMode = transMode;
    }

    @Override
    public String toString() {
        return "MediaTransConfigObject{" +
                "adjDarMethod='" + adjDarMethod + '\'' +
                ", isCheckReso='" + isCheckReso + '\'' +
                ", resoAdjMethod='" + resoAdjMethod + '\'' +
                ", isCheckVideoBitrate='" + isCheckVideoBitrate + '\'' +
                ", videoBitrateAdjMethod='" + videoBitrateAdjMethod + '\'' +
                ", isCheckAudioBitrate='" + isCheckAudioBitrate + '\'' +
                ", audioBitrateAdjMethod='" + audioBitrateAdjMethod + '\'' +
                '}';
    }
}
