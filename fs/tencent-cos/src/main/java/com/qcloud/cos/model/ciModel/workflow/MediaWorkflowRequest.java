package com.qcloud.cos.model.ciModel.workflow;

import com.qcloud.cos.internal.CIWorkflowServiceRequest;

import java.io.Serializable;

/**
 * 工作流请求实体类 请见：https://cloud.tencent.com/document/product/460/45947
 */
public class MediaWorkflowRequest extends CIWorkflowServiceRequest implements Serializable {

    /**
     * 工作流名称 支持中文、英文、数字、—和_，长度限制128字符
     */
    private String name;
    /**
     * 拓扑信息
     */
    private MediaTopology topology;
    /**
     * 工作流状态
     */
    private String state;
    /**
     * 工作流状态
     */
    private String workflowId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MediaTopology getTopology() {
        if (topology == null) {
            topology = new MediaTopology();
        }
        return topology;
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

    public void setTopology(MediaTopology topology) {
        this.topology = topology;
    }
}
