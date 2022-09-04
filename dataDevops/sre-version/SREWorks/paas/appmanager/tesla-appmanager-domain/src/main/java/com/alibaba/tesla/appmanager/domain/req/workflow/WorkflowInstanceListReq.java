package com.alibaba.tesla.appmanager.domain.req.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstanceListReq implements Serializable {

    /**
     * Workflow 实例 ID
     */
    private Long instanceId;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * Workflow 实例状态
     */
    private String workflowStatus;

    /**
     *  Workflow 实例创建者
     */
    private String workflowCreator;
}
