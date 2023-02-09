package com.qcloud.cos.model.ciModel.persistence;

public class ImageInfo {
    private String format;
    private Integer width;
    private Integer height;
    private Integer quality;
    private String ave;
    private Integer orientation;
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getQuality() {
        return quality;
    }

    public void setQuality(Integer quality) {
        this.quality = quality;
    }

    public String getAve() {
        return ave;
    }

    public void setAve(String ave) {
        this.ave = ave;
    }

    public Integer getOrientation() {
        return orientation;
    }

    public void setOrientation(Integer orientation) {
        this.orientation = orientation;
    }
}
