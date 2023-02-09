package com.qcloud.cos.model.ciModel.auditing;


import com.qcloud.cos.internal.CosServiceRequest;

/**
 * 图片审核请求实体 参数详情参考：https://cloud.tencent.com/document/product/460/37318
 */
public class ImageAuditingRequest extends CosServiceRequest {
    /**
     * 审核类型，拥有 porn（涉黄识别）、terrorist（涉暴恐识别）、politics（涉政识别）、ads（广告识别）四种。用户可选择多种识别类型，
     * 例如 detectType=porn,ads 表示对图片进行涉黄及广告审核
     */
    private String detectType;

    /**
     * 操作的bucket名称
     */
    private String bucketName;

    /**
     * ObjectKey 对象在存储桶中的位置及名称
     * 例如根目录下pic文件夹中的1.jpg文件   pic/1.jpg
     */
    private String objectKey;

    /**
     * 截帧频率，GIF 图或长图检测专用，默认值为5，表示只会检测 GIF 图或长图的第一帧
     */
    private int interval = 5;

    /**
     * 最大截帧数量，GIF 图或长图检测专用，默认值为5，表示只取 GIF 的第1帧图片进行审核，或长图不进行切分识别
     */
    private int maxFrames = 5;

    /**
     * 审核策略的唯一标识，由后台自动生成，在控制台中对应为 Biztype 值
     */
    private String bizType;

    /**
     * 您可以通过填写detect-url审核任意公网可访问的图片链接
     * 不填写detect-url时，后台会默认审核ObjectKey
     * 填写了detect-url时，后台会审核detect-url链接，无需再填写ObjectKey
     * detect-url示例：http://www.example.com/abc.jpg
     */
    private String detectUrl;

    public String getDetectType() {
        return detectType;
    }

    public void setDetectType(String detectType) {
        this.detectType = detectType;
    }

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


    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int getMaxFrames() {
        return maxFrames;
    }

    public void setMaxFrames(int maxFrames) {
        this.maxFrames = maxFrames;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getDetectUrl() {
        return detectUrl;
    }

    public void setDetectUrl(String detectUrl) {
        this.detectUrl = detectUrl;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ImageAuditingRequest{");
        sb.append("detectType='").append(detectType).append('\'');
        sb.append(", bucketName='").append(bucketName).append('\'');
        sb.append(", objectKey='").append(objectKey).append('\'');
        sb.append(", interval=").append(interval);
        sb.append(", maxFrames=").append(maxFrames);
        sb.append(", bizType='").append(bizType).append('\'');
        sb.append(", detectUrl='").append(detectUrl).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
