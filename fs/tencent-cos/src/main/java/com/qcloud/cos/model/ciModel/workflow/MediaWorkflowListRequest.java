package com.qcloud.cos.model.ciModel.workflow;

import com.qcloud.cos.internal.CIServiceRequest;

import java.io.Serializable;

/**
 * 工作流列表请求实体类 请见：https://cloud.tencent.com/document/product/460/45947
 */
public class MediaWorkflowListRequest extends CIServiceRequest implements Serializable {
    /**
     * 工作流 ID，以,符号分割字符串
     */
    private String ids;
    /**
     * 名称
     */
    private String name;
    /**
     * 第几页
     */
    private String pageNumber;
    /**
     * 每页个数
     */
    private String pageSize;

    /**
     * 工作流 ID
     */
    private String workflowId;
    /**
     * Desc 或者 Asc。默认为 Desc
     */
    private String orderByTime;
    /**
     * 拉取的最大任务数。默认为10。最大为100
     */
    private String size;
    /**
     * 工作流实例状态，以,分割支持多状态
     * All，Success，Failed，Running，Cancel。默认为 All
     */
    private String states;
    /**
     * 拉取创建时间大于该时间。格式为：%Y-%m-%dT%H:%m:%S%z
     */
    private String startCreationTime;
    /**
     * 拉取创建时间小于该时间。格式为：%Y-%m-%dT%H:%m:%S%z
     */
    private String endCreationTime;
    /**
     * 请求的上下文，用于翻页。下一页输入 token
     */
    private String nextToken;

    /**
     * 工作流实例 ID
     */
    private String runId;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getIds() {
        return ids;
    }

    public void setIds(String ids) {
        this.ids = ids;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getOrderByTime() {
        return orderByTime;
    }

    public void setOrderByTime(String orderByTime) {
        this.orderByTime = orderByTime;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
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

    public String getNextToken() {
        return nextToken;
    }

    public void setNextToken(String nextToken) {
        this.nextToken = nextToken;
    }

    @Override
    public String toString() {
        return "WorkflowRequest{" +
                "ids='" + ids + '\'' +
                ", name='" + name + '\'' +
                ", pageNumber='" + pageNumber + '\'' +
                ", pageSize='" + pageSize + '\'' +
                ", workflowId='" + workflowId + '\'' +
                ", orderByTime='" + orderByTime + '\'' +
                ", size='" + size + '\'' +
                ", states='" + states + '\'' +
                ", startCreationTime='" + startCreationTime + '\'' +
                ", endCreationTime='" + endCreationTime + '\'' +
                ", nextToken='" + nextToken + '\'' +
                '}';
    }

}
