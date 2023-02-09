package com.qcloud.cos.model.ciModel.job;

import com.qcloud.cos.internal.CIServiceRequest;

/**
 * 文档预览任务发起请求类
 */
public class DocJobListRequest extends CIServiceRequest {

    /**
     * cos桶名称
     */
    private String bucketName;

    /**
     * 任务的队列id
     */
    private String queueId;
    /**
     * 任务类型
     */
    private String tag;
    /**
     * 时间顺序
     */
    private String orderByTime;
    /**
     * 下一个token
     */
    private String nextToken;
    /**
     * 查询数量 默认为十个
     */
    private Integer size = 10;
    /**
     * 任务状态
     */
    private String states;
    /**
     * 开始时间
     */
    private String startCreationTime;
    /**
     * 结束时间
     */
    private String endCreationTime;

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getOrderByTime() {
        return orderByTime;
    }

    public void setOrderByTime(String orderByTime) {
        this.orderByTime = orderByTime;
    }

    public String getNextToken() {
        return nextToken;
    }

    public void setNextToken(String nextToken) {
        this.nextToken = nextToken;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getStates() {
        return states;
    }

    public void setStates(String states) {
        this.states = states;
    }

    public String getStartCreationTime() {
        return startCreationTime;
    }

    public void setStartCreationTime(String startCreationTime) {
        this.startCreationTime = startCreationTime;
    }

    public String getEndCreationTime() {
        return endCreationTime;
    }

    public void setEndCreationTime(String endCreationTime) {
        this.endCreationTime = endCreationTime;
    }

    @Override
    public String toString() {
        return "DocJobListRequest{" +
                "bucketName='" + bucketName + '\'' +
                ", queueId='" + queueId + '\'' +
                ", tag='" + tag + '\'' +
                ", orderByTime='" + orderByTime + '\'' +
                ", nextToken='" + nextToken + '\'' +
                ", size=" + size +
                ", states='" + states + '\'' +
                ", startCreationTime='" + startCreationTime + '\'' +
                ", endCreationTime='" + endCreationTime + '\'' +
                '}';
    }
}
