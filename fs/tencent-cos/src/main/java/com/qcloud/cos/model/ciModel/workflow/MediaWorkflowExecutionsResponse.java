package com.qcloud.cos.model.ciModel.workflow;

import com.qcloud.cos.model.CiServiceResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流请求实体类 请见：https://cloud.tencent.com/document/product/460/45947
 */
public class MediaWorkflowExecutionsResponse extends CiServiceResult {

    /**
     * 工作流实例详细信息
     */
    private List<MediaWorkflowExecutionObject> workflowExecutionList;

    private String nextToken;

    private String requestId;

    public List<MediaWorkflowExecutionObject> getWorkflowExecutionList() {
        if (workflowExecutionList==null){
            workflowExecutionList = new ArrayList<>();
        }
        return workflowExecutionList;
    }

    public void setWorkflowExecutionList(List<MediaWorkflowExecutionObject> workflowExecutionList) {
        workflowExecutionList = workflowExecutionList;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getNextToken() {
        return nextToken;
    }

    public void setNextToken(String nextToken) {
        this.nextToken = nextToken;
    }


    @Override
    public String toString() {
        return "MediaWorkflowExecutionsResponse{" +
                "WorkflowExecutionList=" + workflowExecutionList +
                ", nextToken='" + nextToken + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
