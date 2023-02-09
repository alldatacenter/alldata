package com.qcloud.cos.model.ciModel.image;

import com.qcloud.cos.internal.CosServiceRequest;

/**
 * 获取图片标签接口请求实体 https://cloud.tencent.com/document/product/460/39082
 */
public class ImageLabelV2Request extends CosServiceRequest {
    /**
     * 对象在cos中的相对位置，例如 demo/picture.jpg
     */
    private String objectKey;

    /**
     * 本次调用支持的识别场景，默认为web 可选值如下：
     * web，针对网络图片优化;
     * camera，针对手机摄像头拍摄图片优化;
     * lbum，针对手机相册、网盘产品优化;
     * news，针对新闻、资讯、广电等行业优化；
     * nonecam，非实拍图；
     * product，主体位置识别；如果不传此参数，则默认为web。
     * 支持多场景（scenes）一起检测,例如，使用 scenes=web,camera 即对一张图片使用两个模型同时检测，输出两套识别结果。
     */
    private String scenes;

    /**
     * 操作的bucket名称
     */
    private String bucketName;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getScenes() {
        return scenes;
    }

    public void setScenes(String scenes) {
        this.scenes = scenes;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ImageLabelV2Request{");
        sb.append("objectKey='").append(objectKey).append('\'');
        sb.append(", scenes='").append(scenes).append('\'');
        sb.append(", bucketName='").append(bucketName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
