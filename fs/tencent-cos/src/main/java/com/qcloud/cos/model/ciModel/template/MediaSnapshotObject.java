package com.qcloud.cos.model.ciModel.template;


public class MediaSnapshotObject {

    /**
     * 模式
     */
    private String mode ;
    /**
     * 开始时间
     */
    private String start;
    /**
     * 截图频率
     */
    private String timeInterval;
    /**
     * 截图数量
     */
    private String count;
    /**
     * 视频原始宽度
     */
    private String width;
    /**
     * 视频原始高度
     */
    private String height;

    /**
     * 每秒帧数
     */
    private String fps;


    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(String timeInterval) {
        this.timeInterval = timeInterval;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getFps() {
        return fps;
    }

    public void setFps(String fps) {
        this.fps = fps;
    }

    @Override
    public String toString() {
        return "MediaSnapshotObject{" +
                "mode='" + mode + '\'' +
                ", start='" + start + '\'' +
                ", timeInterval='" + timeInterval + '\'' +
                ", count='" + count + '\'' +
                ", width='" + width + '\'' +
                ", height='" + height + '\'' +
                ", fps='" + fps + '\'' +
                '}';
    }
}
