package com.qcloud.cos.model.ciModel.persistence;

import com.qcloud.cos.model.ciModel.recognition.QRcodeInfo;

import java.util.List;

public class CIObject {
    private String key;
    private String location;
    private String format;
    private Integer width;
    private Integer height;
    private Integer size;
    private Integer quality;
    private String etag;
    private Integer watermarkStatus;
    private Integer codeStatus;
    private List<QRcodeInfo> QRcodeInfoList;
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

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

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getQuality() {
        return quality;
    }

    public void setQuality(Integer quality) {
        this.quality = quality;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public Integer getCodeStatus() {
        return codeStatus;
    }

    public void setCodeStatus(Integer codeStatus) {
        this.codeStatus = codeStatus;
    }

    public List<QRcodeInfo> getQRcodeInfoList() {
        return QRcodeInfoList;
    }

    public void setQRcodeInfoList(List<QRcodeInfo> QRcodeInfoList) {
        this.QRcodeInfoList = QRcodeInfoList;
    }

    public Integer getWatermarkStatus() {
        return watermarkStatus;
    }

    public void setWatermarkStatus(Integer watermarkStatus) {
        this.watermarkStatus = watermarkStatus;
    }
}
