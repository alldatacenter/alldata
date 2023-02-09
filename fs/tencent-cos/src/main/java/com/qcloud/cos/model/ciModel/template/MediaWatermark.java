package com.qcloud.cos.model.ciModel.template;

/**
 * 水印模板实体类 参数详情见:https://cloud.tencent.com/document/product/460/48176
 */
public class MediaWatermark {
    /**
     * 水印类型 Text：文字水印 Image：图片水印
     */
    private String type;
    /**
     * 偏移方式 Relativity：按比例 Absolute：固定位置
     */
    private String locMode;
    /**
     * 水平偏移
     */
    private String dx;
    /**
     * 垂直偏移
     */
    private String dy;
    /**
     * 基准位置
     */
    private String pos;
    /**
     * 水印开始时间
     */
    private String startTime;
    /**
     * 水印结束时间
     */
    private String endTime;
    /**
     * 水印text节点
     */
    private MediaWaterMarkText text;

    /**
     * 水印图片节点
     */
    private MediaWaterMarkImage image;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocMode() {
        return locMode;
    }

    public void setLocMode(String locMode) {
        this.locMode = locMode;
    }

    public String getDx() {
        return dx;
    }

    public void setDx(String dx) {
        this.dx = dx;
    }

    public String getDy() {
        return dy;
    }

    public void setDy(String dy) {
        this.dy = dy;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
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

    public MediaWaterMarkText getText() {
        if (text==null){
            text = new MediaWaterMarkText();
        }
        return text;
    }

    public void setText(MediaWaterMarkText text) {
        this.text = text;
    }

    public MediaWaterMarkImage getImage() {
        if (image==null){
            image = new MediaWaterMarkImage();
        }
        return image;
    }

    public void setImage(MediaWaterMarkImage image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return "MediaWatermark{" +
                "type='" + type + '\'' +
                ", locMode='" + locMode + '\'' +
                ", dx='" + dx + '\'' +
                ", dy='" + dy + '\'' +
                ", pos='" + pos + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", text=" + text +
                ", image=" + image +
                '}';
    }
}
