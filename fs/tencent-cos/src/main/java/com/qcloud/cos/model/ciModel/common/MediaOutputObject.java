package com.qcloud.cos.model.ciModel.common;


/**
 * 媒体处理 输出信息实体
 */
public class MediaOutputObject {
    private String region;
    private String bucket;

    /**
     * 对象在桶中的地址
     */
    private String object;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    @Override
    public String toString() {
        return "MediaOutputObject{" +
                "region='" + region + '\'' +
                ", bucket='" + bucket + '\'' +
                ", object='" + object + '\'' +
                '}';
    }
}
