package com.qcloud.cos.model.ciModel.job;

/**
 * 媒体处理拼接节点
 */
public class MediaConcatFragmentObject {
    /**
     * 拼接cos资源完整url
     */
    private String url;
    /**
     * Start：开头
     * End：结尾
     */
    private String mode;
    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MediaConcatFragmentObject{");
        sb.append("url='").append(url).append('\'');
        sb.append(", mode='").append(mode).append('\'');
        sb.append(", startTime='").append(startTime).append('\'');
        sb.append(", endTime='").append(endTime).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
