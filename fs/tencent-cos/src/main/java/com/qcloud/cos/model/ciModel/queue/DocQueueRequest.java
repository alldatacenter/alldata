package com.qcloud.cos.model.ciModel.queue;

import com.qcloud.cos.internal.CIServiceRequest;

import java.io.Serializable;

/**
 * 文档预览 队列请求实体 https://cloud.tencent.com/document/product/460/46947
 */
public class DocQueueRequest extends CIServiceRequest implements Serializable {
    private String queueId;
    private String state;
    private String pageNumber;
    private String pageSize;
    private String name;
    private MediaNotifyConfig notifyConfig = new MediaNotifyConfig();

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(String pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public MediaNotifyConfig getNotifyConfig() {
        return notifyConfig;
    }

    public void setNotifyConfig(MediaNotifyConfig notifyConfig) {
        this.notifyConfig = notifyConfig;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "MediaQueueRequest{" +
                "queueId='" + queueId + '\'' +
                ", state='" + state + '\'' +
                ", pageNumber='" + pageNumber + '\'' +
                ", pageSize='" + pageSize + '\'' +
                ", name='" + name + '\'' +
                ", notifyConfig=" + notifyConfig +
                '}';
    }
}
