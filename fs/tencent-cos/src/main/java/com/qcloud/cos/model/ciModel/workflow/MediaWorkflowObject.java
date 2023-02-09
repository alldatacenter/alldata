package com.qcloud.cos.model.ciModel.workflow;

public class MediaWorkflowObject {

    private String bucketId;
    private String CreateTime;
    private String Name;
    private String State;
    private String UpdateTime;
    private String WorkflowId;
    private MediaTopology topology;

    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String bucketId) {
        this.bucketId = bucketId;
    }

    public String getCreateTime() {
        return CreateTime;
    }

    public void setCreateTime(String createTime) {
        CreateTime = createTime;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getState() {
        return State;
    }

    public void setState(String state) {
        State = state;
    }

    public String getUpdateTime() {
        return UpdateTime;
    }

    public void setUpdateTime(String updateTime) {
        UpdateTime = updateTime;
    }

    public String getWorkflowId() {
        return WorkflowId;
    }

    public void setWorkflowId(String workflowId) {
        WorkflowId = workflowId;
    }

    public MediaTopology getTopology() {
        if (topology==null){
            topology = new MediaTopology();
        }
        return topology;
    }

    public void setTopology(MediaTopology topology) {
        this.topology = topology;
    }

    @Override
    public String toString() {
        return "MediaWorkflowObject{" +
                "bucketId='" + bucketId + '\'' +
                ", CreateTime='" + CreateTime + '\'' +
                ", Name='" + Name + '\'' +
                ", State='" + State + '\'' +
                ", UpdateTime='" + UpdateTime + '\'' +
                ", WorkflowId='" + WorkflowId + '\'' +
                ", topology=" + topology +
                '}';
    }
}
