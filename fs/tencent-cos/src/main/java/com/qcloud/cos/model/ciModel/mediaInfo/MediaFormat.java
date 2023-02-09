package com.qcloud.cos.model.ciModel.mediaInfo;

/**
 * MediaInfo 格式详情实体类 详情见：https://cloud.tencent.com/document/product/460/38935
 */
public class MediaFormat {
    /**
     * 比特率，单位为 kbps
     */
    private String bitrate;
    /**
     * 时长，单位为秒
     */
    private String duration;
    /**
     * 容器格式的详细名称
     */
    private String formatLongName;
    /**
     * 容器格式名称
     */
    private String formatName;
    /**
     * 节目的数量
     */
    private String numProgram;
    /**
     *	Stream（包含 Video、Audio、Subtitle）的数量
     */
    private String numStream;
    /**
     * 大小，单位为 Byte
     */
    private String size;
    /**
     * 起始时间，单位为秒
     */
    private String startTime;

    public String getBitrate() {
        return bitrate;
    }

    public void setBitrate(String bitrate) {
        this.bitrate = bitrate;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getFormatLongName() {
        return formatLongName;
    }

    public void setFormatLongName(String formatLongName) {
        this.formatLongName = formatLongName;
    }

    public String getFormatName() {
        return formatName;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }

    public String getNumProgram() {
        return numProgram;
    }

    public void setNumProgram(String numProgram) {
        this.numProgram = numProgram;
    }

    public String getNumStream() {
        return numStream;
    }

    public void setNumStream(String numStream) {
        this.numStream = numStream;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    @Override
    public String toString() {
        return "MediaFormat{" +
                "bitrate='" + bitrate + '\'' +
                ", duration='" + duration + '\'' +
                ", formatLongName='" + formatLongName + '\'' +
                ", formatName='" + formatName + '\'' +
                ", numProgram='" + numProgram + '\'' +
                ", numStream='" + numStream + '\'' +
                ", size='" + size + '\'' +
                ", startTime='" + startTime + '\'' +
                '}';
    }
}
