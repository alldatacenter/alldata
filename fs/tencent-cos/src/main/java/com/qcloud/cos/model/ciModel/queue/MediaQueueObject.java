package com.qcloud.cos.model.ciModel.queue;

import com.qcloud.cos.internal.CIServiceRequest;

import java.io.Serializable;

/**
 * 媒体处理 队列实体 https://cloud.tencent.com/document/product/460/42324
 */
public class MediaQueueObject extends CIServiceRequest implements Serializable {
    private String queueId;
    private String name;
    private String state;
    private MediaNotifyConfig notifyConfig;
    private String maxSize;
    private String maxConcurrent;
    private String createTime;
    private String updateTime;
    private String category;
    private String bucketId;

    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String bucketId) {
        this.bucketId = bucketId;
    }

    public MediaQueueObject() {
        this.notifyConfig = new MediaNotifyConfig();
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public MediaNotifyConfig getNotifyConfig() {
        if (notifyConfig==null){
            notifyConfig = new MediaNotifyConfig();
        }
        return notifyConfig;
    }

    public void setNotifyConfig(MediaNotifyConfig notifyConfig) {
        this.notifyConfig = notifyConfig;
    }

    public String getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(String maxSize) {
        this.maxSize = maxSize;
    }

    public String getMaxConcurrent() {
        return maxConcurrent;
    }

    public void setMaxConcurrent(String maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "MediaQueueObject{" +
                "queueId='" + queueId + '\'' +
                ", name='" + name + '\'' +
                ", state='" + state + '\'' +
                ", notifyConfig=" + notifyConfig +
                ", maxSize='" + maxSize + '\'' +
                ", maxConcurrent='" + maxConcurrent + '\'' +
                ", createTime='" + createTime + '\'' +
                ", updateTime='" + updateTime + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}
