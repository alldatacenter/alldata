package com.qcloud.cos.model.ciModel.workflow;


import com.qcloud.cos.model.CiServiceResult;

import java.io.Serializable;

/**
 * 工作流请求实体类 请见：https://cloud.tencent.com/document/product/460/45947
 */
public class MediaWorkflowResponse extends CiServiceResult implements Serializable {

    /**
     * 工作流名称 支持中文、英文、数字、—和_，长度限制128字符
     */
    private String name;

    /**
     * bucket id
     */
    private String bucketId;
    /**
     * 拓扑信息
     */
    private MediaTopology topology;
    /**
     * 工作流状态
     */
    private String state;
    /**
     * 工作流id
     */
    private String workflowId;
    /**
     * 创建时间
     */
    private String createTime;
    /**
     * 修改时间
     */
    private String updateTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MediaTopology getTopology() {
        return topology;
    }

    public void setTopology(MediaTopology topology) {
        this.topology = topology;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
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

    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String bucketId) {
        this.bucketId = bucketId;
    }

    @Override
    public String toString() {
        return "MediaWorkflowResponse{" +
                "name='" + name + '\'' +
                ", bucketId='" + bucketId + '\'' +
                ", topology=" + topology +
                ", state='" + state + '\'' +
                ", workflowId='" + workflowId + '\'' +
                ", createTime='" + createTime + '\'' +
                ", updateTime='" + updateTime + '\'' +
                '}';
    }
}
