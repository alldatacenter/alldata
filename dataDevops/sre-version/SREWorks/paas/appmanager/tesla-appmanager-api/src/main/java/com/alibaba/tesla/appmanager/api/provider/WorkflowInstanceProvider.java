package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.WorkflowInstanceDTO;
import com.alibaba.tesla.appmanager.domain.option.WorkflowInstanceOption;
import com.alibaba.tesla.appmanager.domain.req.workflow.WorkflowInstanceListReq;
import com.alibaba.tesla.appmanager.domain.res.workflow.WorkflowInstanceOperationRes;

/**
 * Workflow 实例 Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface WorkflowInstanceProvider {

    /**
     * 根据 Workflow 实例 ID 获取对应的 Workflow 实例
     *
     * @param workflowInstanceId Workflow 实例 ID
     * @param withExt            是否包含扩展信息
     * @return Workflow 实例对象，不存在则返回 null
     */
    WorkflowInstanceDTO get(Long workflowInstanceId, boolean withExt);

    /**
     * 根据条件过滤 Workflow 实例列表
     *
     * @param request 过滤条件
     * @return List
     */
    Pagination<WorkflowInstanceDTO> list(WorkflowInstanceListReq request);

    /**
     * 启动一个 Workflow 实例
     *
     * @param appId         应用 ID
     * @param configuration 启动配置
     * @param options       Workflow 配置项
     * @return 启动后的 Workflow 实例
     */
    WorkflowInstanceDTO launch(String appId, String configuration, WorkflowInstanceOption options);

    /**
     * 恢复处于 SUSPEND 状态的 Workflow 实例
     *
     * @param workflowInstanceId Workflow 实例 ID
     * @return 执行结果
     */
    WorkflowInstanceOperationRes resume(Long workflowInstanceId);

    /**
     * 终止当前 Workflow 实例，并下发 InterruptedException 到 Task 侧
     *
     * @param workflowInstanceId Workflow 实例 ID
     * @return 执行结果
     */
    WorkflowInstanceOperationRes terminate(Long workflowInstanceId);

    /**
     * 重试当前已经到达终态的 Workflow 实例 (SUCCESS/FAILURE/EXCEPTION/TERMINATED)
     * <p>
     * 注意该方法将会从第一个节点开始，使用原始参数重新运行一遍当前 Workflow 实例
     *
     * @param workflowInstanceId Workflow 实例 ID
     * @return 执行结果
     */
    WorkflowInstanceOperationRes retry(Long workflowInstanceId);

    /**
     * 重试当前已经到达终态的 Workflow 实例 (SUCCESS/FAILURE/EXCEPTION/TERMINATED)
     * <p>
     * 注意该方法从指定 taskId 开始进行重试，即重新运行 taskId 及之后的所有 WorkflowInstance 任务
     * <p>
     * 该方法会获取 taskId 对应的快照内容，以此为输入进行重试
     *
     * @param workflowInstanceId Workflow 实例 ID
     * @param workflowTaskId     Workflow Task ID
     * @return 更新状态后的 Workflow 实例
     */
    WorkflowInstanceDTO retryFromTask(Long workflowInstanceId, Long workflowTaskId);
}
