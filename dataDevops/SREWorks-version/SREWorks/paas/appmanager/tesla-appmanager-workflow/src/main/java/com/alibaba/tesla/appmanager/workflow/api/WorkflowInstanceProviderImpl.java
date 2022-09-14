package com.alibaba.tesla.appmanager.workflow.api;

import com.alibaba.tesla.appmanager.api.provider.WorkflowInstanceProvider;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.WorkflowInstanceDTO;
import com.alibaba.tesla.appmanager.domain.option.WorkflowInstanceOption;
import com.alibaba.tesla.appmanager.domain.req.workflow.WorkflowInstanceListReq;
import com.alibaba.tesla.appmanager.workflow.assembly.WorkflowInstanceDtoConvert;
import com.alibaba.tesla.appmanager.workflow.repository.condition.WorkflowInstanceQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowInstanceDO;
import com.alibaba.tesla.appmanager.workflow.service.WorkflowInstanceService;
import com.alibaba.tesla.appmanager.domain.res.workflow.WorkflowInstanceOperationRes;
import com.alibaba.tesla.appmanager.workflow.service.pubsub.WorkflowInstanceOperationCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Workflow 实例 Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class WorkflowInstanceProviderImpl implements WorkflowInstanceProvider {

    @Autowired
    private WorkflowInstanceService workflowInstanceService;

    @Autowired
    private WorkflowInstanceDtoConvert convert;

    /**
     * 根据 Workflow 实例 ID 获取对应的 Workflow 实例
     *
     * @param workflowInstanceId Workflow 实例 ID
     * @param withExt            是否包含扩展信息
     * @return Workflow 实例对象，不存在则返回 null
     */
    @Override
    public WorkflowInstanceDTO get(Long workflowInstanceId, boolean withExt) {
        WorkflowInstanceDO instance = workflowInstanceService.get(workflowInstanceId, withExt);
        return convert.to(instance);
    }

    /**
     * 根据条件过滤 Workflow 实例列表
     *
     * @param request 过滤条件
     * @return List
     */
    @Override
    public Pagination<WorkflowInstanceDTO> list(WorkflowInstanceListReq request) {
        WorkflowInstanceQueryCondition condition = WorkflowInstanceQueryCondition.builder()
                .instanceId(request.getInstanceId())
                .appId(request.getAppId())
                .workflowStatus(request.getWorkflowStatus())
                .workflowCreator(request.getWorkflowCreator())
                .build();
        Pagination<WorkflowInstanceDO> result = workflowInstanceService.list(condition);
        return Pagination.transform(result, item -> convert.to(item));
    }

    /**
     * 启动一个 Workflow 实例
     *
     * @param appId         应用 ID
     * @param configuration 启动配置
     * @param options       Workflow 配置项
     * @return 启动后的 Workflow 实例
     */
    @Override
    public WorkflowInstanceDTO launch(String appId, String configuration, WorkflowInstanceOption options) {
        WorkflowInstanceDO instance = workflowInstanceService.launch(appId, configuration, options);
        return convert.to(instance);
    }

    /**
     * 恢复处于 SUSPEND 状态的 Workflow 实例
     *
     * @param workflowInstanceId Workflow 实例 ID
     * @return 执行结果
     */
    @Override
    public WorkflowInstanceOperationRes resume(Long workflowInstanceId) {
        return workflowInstanceService.broadcastCommand(workflowInstanceId,
                WorkflowInstanceOperationCommand.COMMAND_RESUME);
    }

    /**
     * 终止当前 Workflow 实例，并下发 InterruptedException 到 Task 侧
     *
     * @param workflowInstanceId Workflow 实例 ID
     * @return 执行结果
     */
    @Override
    public WorkflowInstanceOperationRes terminate(Long workflowInstanceId) {
        return workflowInstanceService.broadcastCommand(workflowInstanceId,
                WorkflowInstanceOperationCommand.COMMAND_TERMINATE);
    }

    /**
     * 重试当前已经到达终态的 Workflow 实例 (SUCCESS/FAILURE/EXCEPTION/TERMINATED)
     * <p>
     * 注意该方法将会从第一个节点开始，使用原始参数重新运行一遍当前 Workflow 实例
     *
     * @param workflowInstanceId Workflow 实例 ID
     * @return 执行结果
     */
    @Override
    public WorkflowInstanceOperationRes retry(Long workflowInstanceId) {
        return workflowInstanceService.broadcastCommand(workflowInstanceId,
                WorkflowInstanceOperationCommand.COMMAND_RETRY);
    }

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
    @Override
    public WorkflowInstanceDTO retryFromTask(Long workflowInstanceId, Long workflowTaskId) {
        WorkflowInstanceDO instance = workflowInstanceService.localRetryFromTask(workflowInstanceId, workflowTaskId);
        return convert.to(instance);
    }
}
