package com.alibaba.tesla.appmanager.workflow.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Workflow 快照查询条件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSnapshotQueryCondition extends BaseCondition {

    /**
     * Workflow Snapshot ID
     */
    private Long snapshotId;

    /**
     * Workflow Task ID
     */
    private Long taskId;

    /**
     * Workflow 实例 ID
     */
    private Long instanceId;
}
