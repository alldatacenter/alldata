package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.WorkflowTaskDTO;
import com.alibaba.tesla.appmanager.domain.req.workflow.WorkflowTaskListReq;

import java.util.List;

/**
 * 工作流任务 Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface WorkflowTaskProvider {

    /**
     * 根据 WorkflowTaskID 获取对应的 WorkflowTask 对象
     *
     * @param workflowTaskId WorkflowTaskID
     * @param withExt        是否包含扩展信息
     * @return WorkflowTask 对象，不存在则返回 null
     */
    WorkflowTaskDTO get(Long workflowTaskId, boolean withExt);

    /**
     * 根据条件过滤 Workflow 任务列表
     *
     * @param request 过滤条件
     * @return List of WorkflowTask
     */
    Pagination<WorkflowTaskDTO> list(WorkflowTaskListReq request);

    /**
     * 列出当前所有正在运行中的远程 workflow task
     *
     * @return List or WorkflowTaskDTO
     */
    List<WorkflowTaskDTO> listRunningRemoteTask();

    /**
     * 更新指定的 Workflow 任务实例
     *
     * @param task Workflow 任务实例
     * @return 更新行数
     */
    int update(WorkflowTaskDTO task);

    /**
     * 创建一个 Workflow Task 任务 (不触发)
     *
     * @param task Workflow 任务实例
     * @return 创建后的 WorkflowTask 对象
     */
    WorkflowTaskDTO create(WorkflowTaskDTO task);

    /**
     * 终止指定 Workflow 任务 (x -> TERMINATED)
     *
     * @param workflowTaskId WorkflowTaskID
     * @param extMessage     终止时的扩展信息存储字符串
     */
    void terminate(Long workflowTaskId, String extMessage);

    /**
     * 暂停指定 Workflow 任务 (RUNNING -> RUNNING_SUSPEND / WAITING -> WAITING_SUSPEND)
     *
     * @param workflowTaskId WorkflowTaskID
     * @param extMessage     暂停时的扩展信息存储字符串
     */
    void suspend(Long workflowTaskId, String extMessage);
}
