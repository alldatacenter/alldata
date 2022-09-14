package com.alibaba.tesla.appmanager.domain.req;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建工作流快照请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkflowSnapshotReq {

    /**
     * Workflow 实例 ID (reference am_workflow_instance.id)
     */
    private Long workflowInstanceId;

    /**
     * Workflow 任务 ID
     */
    private Long workflowTaskId;

    /**
     * Workflow 运行上下文
     */
    private JSONObject context;

    /**
     * Workflow 运行 Configuration 渲染后对象
     */
    private DeployAppSchema configuration;
}
