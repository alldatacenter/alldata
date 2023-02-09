package com.qcloud.cos.model.ciModel.workflow;

public class MediaTasks {
    private String type;
    private String createTime;
    private String endTime;
    private String state;
    private String jobId;
    private String name;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "MediaTasks{" +
                "type='" + type + '\'' +
                ", createTime='" + createTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", state='" + state + '\'' +
                ", jobId='" + jobId + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
