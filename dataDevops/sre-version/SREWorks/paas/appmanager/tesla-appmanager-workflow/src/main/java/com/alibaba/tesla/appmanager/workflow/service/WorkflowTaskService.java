package com.alibaba.tesla.appmanager.workflow.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.workflow.repository.condition.WorkflowTaskQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowInstanceDO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowTaskDO;

import java.util.List;

/**
 * 工作流任务 服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface WorkflowTaskService {

    /**
     * 根据 WorkflowTaskID 获取对应的 WorkflowTask 对象
     *
     * @param workflowTaskId WorkflowTaskID
     * @param withExt        是否包含扩展信息
     * @return WorkflowTask 对象，不存在则返回 null
     */
    WorkflowTaskDO get(Long workflowTaskId, boolean withExt);

    /**
     * 根据条件过滤 Workflow 任务列表
     *
     * @param condition 过滤条件
     * @return List of WorkflowTask
     */
    Pagination<WorkflowTaskDO> list(WorkflowTaskQueryCondition condition);

    /**
     * 列出当前所有正在运行中的远程 workflow task
     *
     * @return List or WorkflowTaskDO
     */
    List<WorkflowTaskDO> listRunningRemoteTask();

    /**
     * 更新指定的 Workflow 任务实例
     *
     * @param task Workflow 任务实例
     * @return 更新行数
     */
    int update(WorkflowTaskDO task);

    /**
     * 创建一个 Workflow Task 任务 (不触发)
     *
     * @param task Workflow 任务实例
     * @return 创建后的 WorkflowTask 对象
     */
    WorkflowTaskDO create(WorkflowTaskDO task);

    /**
     * 触发执行一个 Workflow Task 任务，并等待其完成 (PENDING -> RUNNING)
     *
     * @param instance Workflow 实例
     * @param task     Workflow 任务
     * @param context  上下文信息
     * @return 携带运行信息的 WorkflowTaskDO 实例 (未落库，实例 DO 仅在 events 转换时落库)
     */
    WorkflowTaskDO execute(WorkflowInstanceDO instance, WorkflowTaskDO task, JSONObject context);

    /**
     * 终止指定 Workflow 任务 (x -> TERMINATED)
     *
     * @param workflowTaskId WorkflowTaskID
     * @param extMessage     终止时的扩展信息存储字符串
     */
    boolean terminate(Long workflowTaskId, String extMessage);

    /**
     * 暂停指定 Workflow 任务 (RUNNING -> RUNNING_SUSPEND / WAITING -> WAITING_SUSPEND)
     *
     * @param workflowTaskId WorkflowTaskID
     * @param extMessage     暂停时的扩展信息存储字符串
     */
    void suspend(Long workflowTaskId, String extMessage);
}
