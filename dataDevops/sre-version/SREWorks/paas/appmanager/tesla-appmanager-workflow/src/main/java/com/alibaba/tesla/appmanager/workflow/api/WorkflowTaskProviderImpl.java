package com.alibaba.tesla.appmanager.workflow.api;

import com.alibaba.tesla.appmanager.api.provider.WorkflowTaskProvider;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.WorkflowTaskDTO;
import com.alibaba.tesla.appmanager.domain.req.workflow.WorkflowTaskListReq;
import com.alibaba.tesla.appmanager.workflow.assembly.WorkflowTaskDtoConvert;
import com.alibaba.tesla.appmanager.workflow.repository.condition.WorkflowTaskQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowTaskDO;
import com.alibaba.tesla.appmanager.workflow.service.WorkflowTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流任务 Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class WorkflowTaskProviderImpl implements WorkflowTaskProvider {

    @Autowired
    private WorkflowTaskService workflowTaskService;

    @Autowired
    private WorkflowTaskDtoConvert convert;

    /**
     * 根据 WorkflowTaskID 获取对应的 WorkflowTask 对象
     *
     * @param workflowTaskId WorkflowTaskID
     * @param withExt        是否包含扩展信息
     * @return WorkflowTask 对象，不存在则返回 null
     */
    @Override
    public WorkflowTaskDTO get(Long workflowTaskId, boolean withExt) {
        WorkflowTaskDO task = workflowTaskService.get(workflowTaskId, withExt);
        return convert.to(task);
    }

    /**
     * 根据条件过滤 Workflow 任务列表
     *
     * @param request 过滤条件
     * @return List of WorkflowTask
     */
    @Override
    public Pagination<WorkflowTaskDTO> list(WorkflowTaskListReq request) {
        Pagination<WorkflowTaskDO> tasks = workflowTaskService.list(WorkflowTaskQueryCondition.builder()
                .taskId(request.getTaskId())
                .instanceId(request.getInstanceId())
                .appId(request.getAppId())
                .taskType(request.getTaskType())
                .taskStatus(request.getTaskStatus())
                .deployAppId(request.getDeployAppId())
                .build());
        return Pagination.transform(tasks, item -> convert.to(item));
    }

    /**
     * 列出当前所有正在运行中的远程 workflow task
     *
     * @return List or WorkflowTaskDTO
     */
    @Override
    public List<WorkflowTaskDTO> listRunningRemoteTask() {
        return workflowTaskService.listRunningRemoteTask().stream()
                .map(item -> convert.to(item))
                .collect(Collectors.toList());
    }

    /**
     * 更新指定的 Workflow 任务实例
     *
     * @param task Workflow 任务实例
     * @return 更新行数
     */
    @Override
    public int update(WorkflowTaskDTO task) {
        return workflowTaskService.update(convert.from(task));
    }

    /**
     * 创建一个 Workflow Task 任务 (不触发)
     *
     * @param task Workflow 任务实例
     * @return 创建后的 WorkflowTask 对象
     */
    @Override
    public WorkflowTaskDTO create(WorkflowTaskDTO task) {
        return convert.to(workflowTaskService.create(convert.from(task)));
    }

    /**
     * 终止指定 Workflow 任务 (x -> TERMINATED)
     *
     * @param workflowTaskId WorkflowTaskID
     * @param extMessage     终止时的扩展信息存储字符串
     */
    @Override
    public void terminate(Long workflowTaskId, String extMessage) {
        workflowTaskService.terminate(workflowTaskId, extMessage);
    }

    /**
     * 暂停指定 Workflow 任务 (RUNNING -> RUNNING_SUSPEND / WAITING -> WAITING_SUSPEND)
     *
     * @param workflowTaskId WorkflowTaskID
     * @param extMessage     暂停时的扩展信息存储字符串
     */
    @Override
    public void suspend(Long workflowTaskId, String extMessage) {
        workflowTaskService.suspend(workflowTaskId, extMessage);
    }
}
