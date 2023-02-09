package com.qcloud.cos.model.ciModel.job;

import java.io.Serializable;

/**
 * 媒体处理 Audio实体 https://cloud.tencent.com/document/product/460/48234
 */

public class MediaAudioObject implements Serializable {

    /**
     * 编解码格式
     */
    private String codec;
    /**
     * 采样率
     */
    private String samplerate;
    /**
     * 原始音频码率
     */
    private String bitrate;
    /**
     * 声道数
     */
    private String channels;
    /**
     * 是否删除音频流
     */
    private String remove;

    /**
     * 是否删除音频流
     */
    private String profile;

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getSamplerate() {
        return samplerate;
    }

    public void setSamplerate(String samplerate) {
        this.samplerate = samplerate;
    }

    public String getBitrate() {
        return bitrate;
    }

    public void setBitrate(String bitrate) {
        this.bitrate = bitrate;
    }

    public String getChannels() {
        return channels;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }

    public String getRemove() {
        return remove;
    }

    public void setRemove(String remove) {
        this.remove = remove;
    }

    @Override
    public String toString() {
        return "MediaAudioObject{" +
                "codec='" + codec + '\'' +
                ", samplerate='" + samplerate + '\'' +
                ", bitrate='" + bitrate + '\'' +
                ", channels='" + channels + '\'' +
                ", remove='" + remove + '\'' +
                ", profile='" + profile + '\'' +
                '}';
    }
}
