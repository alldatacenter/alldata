package com.alibaba.tesla.appmanager.domain.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 删除工作流快照请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteWorkflowSnapshotReq {

    /**
     * Workflow 实例 ID (reference am_workflow_instance.id)
     */
    private Long workflowInstanceId;

    /**
     * Workflow 任务 ID
     */
    private Long workflowTaskId;
}
