package com.qcloud.cos.model.ciModel.image;

import com.qcloud.cos.internal.CosServiceRequest;

/**
 * 获取图片标签接口请求实体 https://cloud.tencent.com/document/product/460/39082
 */
public class ImageLabelRequest extends CosServiceRequest {
    /**
     * 对象在cos中的相对位置，例如 demo/picture.jpg
     */
    private String objectKey;

    /**
     * 操作的bucket名称
     */
    private String bucketName;

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
}
