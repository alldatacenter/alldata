package com.alibaba.tesla.appmanager.workflow.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Workflow 实例查询条件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstanceQueryCondition extends BaseCondition {

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
