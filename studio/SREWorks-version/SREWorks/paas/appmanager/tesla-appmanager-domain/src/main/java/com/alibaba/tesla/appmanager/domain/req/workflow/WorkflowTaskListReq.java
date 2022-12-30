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
public class WorkflowTaskListReq implements Serializable {

    /**
     * Workflow Task ID
     */
    private Long taskId;

    /**
     * Workflow 实例 ID
     */
    private Long instanceId;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * Task 类型
     */
    private String taskType;

    /**
     * Task 状态
     */
    private String taskStatus;

    /**
     * 部署单 ID
     */
    private Long deployAppId;
}
