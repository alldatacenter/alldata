package com.qcloud.cos.model.ciModel.bucket;

import java.util.Date;

/**
 *  数据万象bucket实体类 字段详情见 https://cloud.tencent.com/document/product/460/38914
 */
public class CIBucketObject {
    /**
     * bucket id
     */
    private String bucketId;
    /**
     * 地域
     */
    private String region;

    /**
     * 创建时间
     */
    private Date createTime;

    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String bucketId) {
        this.bucketId = bucketId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "CIBucketObject{" +
                "bucketId='" + bucketId + '\'' +
                ", region='" + region + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
