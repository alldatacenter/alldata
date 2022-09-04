package com.alibaba.tesla.appmanager.workflow.repository.domain;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Workflow 快照表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowSnapshotDO {
    /**
     * ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * Workflow 实例 ID (reference am_workflow_instance.id)
     */
    private Long workflowInstanceId;

    /**
     * Workflow 任务 ID
     */
    private Long workflowTaskId;

    /**
     * 快照内容 Context
     */
    private String snapshotContext;

    /**
     * 快照内容 Task
     */
    private String snapshotTask;

    /**
     * 快照内容 Workflow
     */
    private String snapshotWorkflow;

    /**
     * 乐观锁版本
     */
    private Integer lockVersion;
}