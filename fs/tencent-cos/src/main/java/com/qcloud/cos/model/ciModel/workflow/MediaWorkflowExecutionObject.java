package com.qcloud.cos.model.ciModel.workflow;


import java.util.LinkedList;

/**
 * 工作流请求实体类 请见：https://cloud.tencent.com/document/product/460/45947
 */
public class MediaWorkflowExecutionObject {
    /**
     * 工作流实例 ID
     */
    private String runId;
    /**
     * 工作流 ID
     */
    private String workflowId;
    /**
     * 工作流名称
     */
    private String workflowName;
    /**
     * 工作流实例状态
     */
    private String state;
    /**
     * 创建时间
     */
    private String createTime;
    /**
     * cos对象地址
     */
    private String object;
    /**
     * 拓扑信息
     */
    private MediaTopology topology;
    /**
     * cos对象地址
     */
    private LinkedList<MediaTasks> tasks;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public MediaTopology getTopology() {
        if (topology == null) {
            topology = new MediaTopology();
        }
        return topology;
    }

    public void setTopology(MediaTopology topology) {
        this.topology = topology;
    }

    public LinkedList<MediaTasks> getTasks() {
        if (tasks == null) {
            tasks = new LinkedList<>();
        }
        return tasks;
    }

    public void setTasks(LinkedList<MediaTasks> tasks) {
        this.tasks = tasks;
    }

    @Override
    public String toString() {
        return "MediaWorkflowExecutionObject{" +
                "runId='" + runId + '\'' +
                ", workflowId='" + workflowId + '\'' +
                ", workflowName='" + workflowName + '\'' +
                ", state='" + state + '\'' +
                ", createTime='" + createTime + '\'' +
                ", object='" + object + '\'' +
                ", topology=" + topology +
                ", tasks=" + tasks +
                '}';
    }
}
