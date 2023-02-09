package com.qcloud.cos.model.ciModel.mediaInfo;


/**
 * MediaInfo video详情实体类 详情见：https://cloud.tencent.com/document/product/460/38935
 */
public class MediaInfoVideo {
    /**
     * 该流的编号
     */
    private String index;
    /**
     * 编解码格式名称
     */
    private String codecName;
    /**
     * 编解码格式的详细名称
     */
    private String codecLongName;
    /**
     * 编码时基
     */
    private String codecTimeBase;
    /**
     * 编码标签名
     */
    private String codecTagString;
    /**
     * 编码标签
     */
    private String codecTag;
    /**
     * 视频编码档位
     */
    private String profile;
    /**
     * 视频高
     */
    private String height;
    /**
     * 视频宽
     */
    private String width;
    /**
     * 是否有B帧
     */
    private String hasBFrame;
    /**
     * 视频编码的参考帧个数
     */
    private String refFrames;
    /**
     * 采样宽高比
     */
    private String sar;
    /**
     * 显示宽高比
     */
    private String dar;
    /**
     * 像素格式
     */
    private String pixFormat;
    /**
     * 场的顺序
     */
    private String fieldOrder;
    /**
     * 视频编码等级
     */
    private String level;
    /**
     * 视频帧率
     */
    private String fps;
    /**
     * 平均帧率
     */
    private String avgFps;
    /**
     * 时基
     */
    private String timebase;
    /**
     * 视频开始时间，单位为秒
     */
    private String startTime;
    /**
     * 视频时长，单位为秒
     */
    private String duration;
    /**
     * 比特率，单位为 kbps
     */
    private String bitrate;
    /**
     * 总帧数
     */
    private String numFrames;
    /**
     * 语言
     */
    private String language;

    private String rotation;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getCodecName() {
        return codecName;
    }

    public void setCodecName(String codecName) {
        this.codecName = codecName;
    }

    public String getCodecLongName() {
        return codecLongName;
    }

    public void setCodecLongName(String codecLongName) {
        this.codecLongName = codecLongName;
    }

    public String getCodecTimeBase() {
        return codecTimeBase;
    }

    public void setCodecTimeBase(String codecTimeBase) {
        this.codecTimeBase = codecTimeBase;
    }

    public String getCodecTagString() {
        return codecTagString;
    }

    public void setCodecTagString(String codecTagString) {
        this.codecTagString = codecTagString;
    }

    public String getCodecTag() {
        return codecTag;
    }

    public void setCodecTag(String codecTag) {
        this.codecTag = codecTag;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getHasBFrame() {
        return hasBFrame;
    }

    public void setHasBFrame(String hasBFrame) {
        this.hasBFrame = hasBFrame;
    }

    public String getRefFrames() {
        return refFrames;
    }

    public void setRefFrames(String refFrames) {
        this.refFrames = refFrames;
    }

    public String getSar() {
        return sar;
    }

    public void setSar(String sar) {
        this.sar = sar;
    }

    public String getDar() {
        return dar;
    }

    public void setDar(String dar) {
        this.dar = dar;
    }

    public String getPixFormat() {
        return pixFormat;
    }

    public void setPixFormat(String pixFormat) {
        this.pixFormat = pixFormat;
    }

    public String getFieldOrder() {
        return fieldOrder;
    }

    public void setFieldOrder(String fieldOrder) {
        this.fieldOrder = fieldOrder;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getFps() {
        return fps;
    }

    public void setFps(String fps) {
        this.fps = fps;
    }

    public String getAvgFps() {
        return avgFps;
    }

    public void setAvgFps(String avgFps) {
        this.avgFps = avgFps;
    }

    public String getTimebase() {
        return timebase;
    }

    public void setTimebase(String timebase) {
        this.timebase = timebase;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getBitrate() {
        return bitrate;
    }

    public void setBitrate(String bitrate) {
        this.bitrate = bitrate;
    }

    public String getNumFrames() {
        return numFrames;
    }

    public void setNumFrames(String numFrames) {
        this.numFrames = numFrames;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getRotation() {
        return rotation;
    }

    public void setRotation(String rotation) {
        this.rotation = rotation;
    }

    @Override
    public String toString() {
        return "MediaInfoVideo{" +
                "index='" + index + '\'' +
                ", codecName='" + codecName + '\'' +
                ", codecLongName='" + codecLongName + '\'' +
                ", codecTimeBase='" + codecTimeBase + '\'' +
                ", codecTagString='" + codecTagString + '\'' +
                ", codecTag='" + codecTag + '\'' +
                ", profile='" + profile + '\'' +
                ", height='" + height + '\'' +
                ", width='" + width + '\'' +
                ", hasBFrame='" + hasBFrame + '\'' +
                ", refFrames='" + refFrames + '\'' +
                ", sar='" + sar + '\'' +
                ", dar='" + dar + '\'' +
                ", pixFormat='" + pixFormat + '\'' +
                ", fieldOrder='" + fieldOrder + '\'' +
                ", level='" + level + '\'' +
                ", fps='" + fps + '\'' +
                ", avgFps='" + avgFps + '\'' +
                ", timebase='" + timebase + '\'' +
                ", startTime='" + startTime + '\'' +
                ", duration='" + duration + '\'' +
                ", bitrate='" + bitrate + '\'' +
                ", numFrames='" + numFrames + '\'' +
                ", language='" + language + '\'' +
                '}';
    }
}
