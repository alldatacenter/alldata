package com.alibaba.tesla.appmanager.workflow.service;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.option.WorkflowInstanceOption;
import com.alibaba.tesla.appmanager.workflow.repository.condition.WorkflowInstanceQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowInstanceDO;
import com.alibaba.tesla.appmanager.domain.res.workflow.WorkflowInstanceOperationRes;

/**
 * 工作流实例服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface WorkflowInstanceService {

    /**
     * 根据 Workflow 实例 ID 获取对应的 Workflow 实例
     *
     * @param workflowInstanceId Workflow 实例 ID
     * @param withExt            是否包含扩展信息
     * @return Workflow 实例对象，不存在则返回 null
     */
    WorkflowInstanceDO get(Long workflowInstanceId, boolean withExt);

    /**
     * 根据条件过滤 Workflow 实例列表
     *
     * @param condition 过滤条件
     * @return List
     */
    Pagination<WorkflowInstanceDO> list(WorkflowInstanceQueryCondition condition);

    /**
     * 启动一个 Workflow 实例
     *
     * @param appId         应用 ID
     * @param configuration 启动配置
     * @param options       Workflow 配置项
     * @return 启动后的 Workflow 实例
     */
    WorkflowInstanceDO launch(String appId, String configuration, WorkflowInstanceOption options);

    /**
     * 更新一个 Workflow 实例 (by Primary Key)
     *
     * @param workflow Workflow 实例
     */
    void update(WorkflowInstanceDO workflow);

    /**
     * 触发指定 Workflow 实例的下一个内部任务的执行
     * <p>
     * 该方法用于 WorkflowInstanceTask 在以 SUCCESS 完成时触发下一个 Task 的执行，并更新 Workflow 实例状态
     *
     * @param instance       Workflow 实例
     * @param workflowTaskId 当前 Workflow Instance 下已经执行成功的最后一个 workflowTaskId
     */
    void triggerNextPendingTask(WorkflowInstanceDO instance, Long workflowTaskId);

    /**
     * 对指定 workflow instance 触发失败事件
     *
     * @param instance     WorkflowInstance
     * @param errorMessage 错误信息
     */
    void triggerProcessFailed(WorkflowInstanceDO instance, String errorMessage);

    /**
     * 对指定 workflow instance 触发异常事件
     *
     * @param instance     WorkflowInstance
     * @param errorMessage 错误信息
     */
    void triggerProcessUnknownError(WorkflowInstanceDO instance, String errorMessage);

    /**
     * 触发指定 Workflow 实例的状态更新，根据当前所有子 Task 的状态进行汇聚更新
     * <p>
     * 该方法用于 WorkflowInstanceTask 在以非 SUCCESS 完成时触发 Workflow 实例状态更新
     *
     * @param workflowInstanceId Workflow 实例 ID
     * @return 更新状态后的 Workflow 实例
     */
    WorkflowInstanceDO triggerUpdate(Long workflowInstanceId);

    /**
     * 对指定 workflow instance 触发终止事件
     *
     * @param instance     WorkflowInstance
     * @param errorMessage 错误信息
     */
    void triggerOpTerminate(WorkflowInstanceDO instance, String errorMessage);

    /**
     * 对指定 workflow instance 触发暂停事件
     *
     * @param instance WorkflowInstance
     */
    void triggerPause(WorkflowInstanceDO instance);

    /**
     * 广播操作: 终止当前 Workflow 实例
     * <p>
     * 等待结果最长 5s。如果没有响应，那么尝试将当前 workflow instance Owner 更新为自身
     * <p>
     * 如果更新 Owner 成功，执行强制终止；更新 Owner 失败则报错 (可能网络分区)
     *
     * @param workflowInstanceId Workflow 实例 ID
     * @param command 命令
     * @return 操作执行结果
     */
    WorkflowInstanceOperationRes broadcastCommand(Long workflowInstanceId, String command);

    /**
     * 当前节点操作: 终止当前 Workflow 实例
     *
     * @param instance Workflow 实例
     * @return 是否成功 (本地存在任务且响应了终止信号返回 true，其他可静默情况返回 false，非法情况抛异常)
     */
    boolean localTerminate(WorkflowInstanceDO instance);

    /**
     * 恢复处于 SUSPEND 状态的 Workflow 实例
     *
     * @param instance Workflow 实例
     * @return 是否成功 (本地存在任务且响应了终止信号返回 true，其他可静默情况返回 false，非法情况抛异常)
     */
    boolean localResume(WorkflowInstanceDO instance);

    /**
     * 重试当前已经到达终态的 Workflow 实例 (SUCCESS/FAILURE/EXCEPTION/TERMINATED)
     * <p>
     * 注意该方法将会从第一个节点开始，使用原始参数重新运行一遍当前 Workflow 实例
     *
     * @param instance Workflow 实例
     * @return 是否成功 (本地存在任务且响应了终止信号返回 true，其他可静默情况返回 false，非法情况抛异常)
     */
    boolean localRetry(WorkflowInstanceDO instance);

    /**
     * 当前节点操作: 重试当前已经到达终态的 Workflow 实例 (SUCCESS/FAILURE/EXCEPTION/TERMINATED)
     * <p>
     * 注意该方法从指定 taskId 开始进行重试，即重新运行 taskId 及之后的所有 WorkflowInstance 任务
     * <p>
     * 该方法会获取 taskId 对应的快照内容，以此为输入进行重试
     *
     * @param workflowInstanceId Workflow 实例 ID
     * @param workflowTaskId     Workflow Task ID
     * @return 更新状态后的 Workflow 实例
     */
    WorkflowInstanceDO localRetryFromTask(Long workflowInstanceId, Long workflowTaskId);
}
