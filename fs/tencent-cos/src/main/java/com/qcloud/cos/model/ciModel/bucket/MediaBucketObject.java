package com.qcloud.cos.model.ciModel.bucket;

/**
 * 数据万象媒体bucket实体类 字段详情见 https://cloud.tencent.com/document/product/460/38914
 */
public class MediaBucketObject {
    private String bucketId;
    private String Name;
    private String Region;
    private String CreateTime;
    private String aliasBucketId;

    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String bucketId) {
        this.bucketId = bucketId;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getRegion() {
        return Region;
    }

    public void setRegion(String region) {
        Region = region;
    }

    public String getCreateTime() {
        return CreateTime;
    }

    public void setCreateTime(String createTime) {
        CreateTime = createTime;
    }

    public String getAliasBucketId() {
        return aliasBucketId;
    }

    public void setAliasBucketId(String aliasBucketId) {
        this.aliasBucketId = aliasBucketId;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("MediaBucketObject{");
        sb.append("bucketId='").append(bucketId).append('\'');
        sb.append(", Name='").append(Name).append('\'');
        sb.append(", Region='").append(Region).append('\'');
        sb.append(", CreateTime='").append(CreateTime).append('\'');
        sb.append(", aliasBucketId='").append(aliasBucketId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
