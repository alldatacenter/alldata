package com.alibaba.tesla.appmanager.workflow.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.autoconfig.ThreadPoolProperties;
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum;
import com.alibaba.tesla.appmanager.common.enums.WorkflowTaskStateEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.req.UpdateWorkflowSnapshotReq;
import com.alibaba.tesla.appmanager.domain.req.workflow.ExecuteWorkflowHandlerReq;
import com.alibaba.tesla.appmanager.domain.res.workflow.ExecuteWorkflowHandlerRes;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandlerFactory;
import com.alibaba.tesla.appmanager.workflow.dynamicscript.WorkflowHandler;
import com.alibaba.tesla.appmanager.workflow.repository.WorkflowTaskRepository;
import com.alibaba.tesla.appmanager.workflow.repository.condition.WorkflowTaskQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowInstanceDO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowSnapshotDO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowTaskDO;
import com.alibaba.tesla.appmanager.workflow.service.WorkflowSnapshotService;
import com.alibaba.tesla.appmanager.workflow.service.WorkflowTaskService;
import com.alibaba.tesla.appmanager.workflow.service.thread.ExecuteWorkflowTaskResult;
import com.alibaba.tesla.appmanager.workflow.service.thread.ExecuteWorkflowTaskWaitingObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Workflow Task 服务实现
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class WorkflowTaskServiceImpl implements WorkflowTaskService {

    @Autowired
    private WorkflowSnapshotService workflowSnapshotService;

    @Autowired
    private WorkflowTaskRepository workflowTaskRepository;

    @Autowired
    private GroovyHandlerFactory groovyHandlerFactory;

    @Autowired
    private ThreadPoolProperties threadPoolProperties;

    /**
     * Workflow Task 线程池
     */
    private ThreadPoolExecutor threadPoolExecutor;

    private final Object threadPoolExecutorLock = new Object();

    @PostConstruct
    public void init() {
        synchronized (threadPoolExecutorLock) {
            threadPoolExecutor = new ThreadPoolExecutor(
                    threadPoolProperties.getWorkflowTaskCoreSize(),
                    threadPoolProperties.getWorkflowTaskMaxSize(),
                    threadPoolProperties.getWorkflowTaskKeepAlive(), TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(threadPoolProperties.getWorkflowTaskQueueCapacity()),
                    r -> new Thread(r, "workflow-task-" + r.hashCode()),
                    new ThreadPoolExecutor.AbortPolicy()
            );
        }
    }

    /**
     * 根据 WorkflowTaskID 获取对应的 WorkflowTask 对象
     *
     * @param workflowTaskId WorkflowTaskID
     * @param withExt        是否包含扩展信息
     * @return WorkflowTask 对象，不存在则返回 null
     */
    @Override
    public WorkflowTaskDO get(Long workflowTaskId, boolean withExt) {
        WorkflowTaskQueryCondition condition = WorkflowTaskQueryCondition.builder()
                .taskId(workflowTaskId)
                .withBlobs(withExt)
                .build();
        return workflowTaskRepository.getByCondition(condition);
    }

    /**
     * 根据条件过滤 Workflow 任务列表
     *
     * @param condition 过滤条件
     * @return List of WorkflowTask
     */
    @Override
    public Pagination<WorkflowTaskDO> list(WorkflowTaskQueryCondition condition) {
        List<WorkflowTaskDO> result = workflowTaskRepository.selectByCondition(condition);
        return Pagination.valueOf(result, Function.identity());
    }

    /**
     * 列出当前所有正在运行中的远程 workflow task
     *
     * @return List or WorkflowTaskDO
     */
    @Override
    public List<WorkflowTaskDO> listRunningRemoteTask() {
        return workflowTaskRepository.listRunningRemoteTask();
    }

    /**
     * 更新指定的 Workflow 任务实例
     *
     * @param task Workflow 任务实例
     * @return 更新行数
     */
    @Override
    public int update(WorkflowTaskDO task) {
        log.info("action=updateWorkflowTask|workflowTaskId={}|workflowInstanceId={}|appId={}|status={}",
                task.getId(), task.getWorkflowInstanceId(), task.getAppId(), task.getTaskStatus());
        return workflowTaskRepository.updateByPrimaryKey(task);
    }

    /**
     * 创建一个 Workflow Task 任务 (不触发, 到 PENDING 状态)
     *
     * @param task Workflow 任务实例
     * @return 创建后的 WorkflowTask 对象
     */
    @Override
    public WorkflowTaskDO create(WorkflowTaskDO task) {
        workflowTaskRepository.insert(task);
        return get(task.getId(), true);
    }

    /**
     * 触发执行一个 Workflow Task 任务，并等待其完成 (PENDING -> RUNNING)
     *
     * @param instance Workflow 实例
     * @param task     Workflow 任务
     * @param context  上下文信息
     * @return 携带运行信息的 WorkflowTaskDO 实例 (未落库，实例 DO 仅在 events 转换时落库)
     */
    @Override
    public WorkflowTaskDO execute(WorkflowInstanceDO instance, WorkflowTaskDO task, JSONObject context) {
        synchronized (threadPoolExecutorLock) {
            if (threadPoolExecutor == null) {
                throw new AppException(AppErrorCode.NOT_READY, "system not ready");
            }
        }

        DeployAppSchema configuration = SchemaUtil.toSchema(DeployAppSchema.class, instance.getWorkflowConfiguration());
        ExecuteWorkflowTaskWaitingObject waitingObject = ExecuteWorkflowTaskWaitingObject.create(task.getId());
        threadPoolExecutor.submit(() -> {
            WorkflowHandler handler;
            try {
                handler = groovyHandlerFactory.get(WorkflowHandler.class,
                        DynamicScriptKindEnum.WORKFLOW.toString(), task.getTaskType());
            } catch (Exception e) {
                log.warn("cannot find workflow handler by taskType {}|workflowInstanceId={}|workflowTaskId={}",
                        task.getTaskType(), task.getWorkflowInstanceId(), task.getId());
                ExecuteWorkflowTaskWaitingObject.triggerFinished(
                        task.getId(),
                        ExecuteWorkflowTaskResult.builder().success(false).extMessage(e.toString()).build());
                return;
            }
            ExecuteWorkflowHandlerReq req = ExecuteWorkflowHandlerReq.builder()
                    .appId(task.getAppId())
                    .instanceId(task.getWorkflowInstanceId())
                    .taskId(task.getId())
                    .taskType(task.getTaskType())
                    .taskStage(task.getTaskStage())
                    .taskProperties(JSONObject.parseObject(task.getTaskProperties()))
                    .context(context)
                    .configuration(configuration)
                    .build();
            ExecuteWorkflowHandlerRes res;
            try {
                res = handler.execute(req);
            } catch (InterruptedException e) {
                ExecuteWorkflowTaskWaitingObject.triggerTerminated(task.getId(), "terminated by InterruptedException");
                return;
            } catch (Throwable e) {
                ExecuteWorkflowTaskWaitingObject.triggerFinished(
                        task.getId(),
                        ExecuteWorkflowTaskResult.builder().task(task).success(false).extMessage(e.toString()).build());
                return;
            }
            ExecuteWorkflowTaskWaitingObject.triggerFinished(
                    task.getId(),
                    ExecuteWorkflowTaskResult.builder().task(task).success(true).output(res).build());
        });
        ExecuteWorkflowTaskResult result;

        // 等待 Task 任务运行完成
        try {
            result = waitingObject.wait(() -> {
                // 上报心跳
                WorkflowTaskDO current = get(task.getId(), false);
                int count = workflowTaskRepository.updateByPrimaryKey(current);
                if (count == 0) {
                    log.warn("failed to report workflow task heartbeat because of lock version expired|" +
                            "workflowTaskId={}", task.getId());
                } else {
                    log.info("workflow task has been reported heartbeat|workflowTaskId={}", task.getId());
                }
            }, 5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return markAbnormalWorkflowTask(task.getId(), WorkflowTaskStateEnum.EXCEPTION, e.toString());
        }

        // 如果被终止或未运行成功，保存错误信息到 workflow task 中
        if (result.isTerminated()) {
            return markAbnormalWorkflowTask(task.getId(), WorkflowTaskStateEnum.TERMINATED, result.getExtMessage());
        } else if (result.isPaused()) {
            return markAbnormalWorkflowTask(task.getId(), WorkflowTaskStateEnum.RUNNING_SUSPEND, result.getExtMessage());
        } else if (!result.isSuccess()) {
            return markAbnormalWorkflowTask(task.getId(), WorkflowTaskStateEnum.FAILURE, result.getExtMessage());
        }

        // 创建返回结果；如果 workflow task 节点主动触发 suspend，那么直接触发进入 WAITING_SUSPEND
        ExecuteWorkflowHandlerRes output = result.getOutput();
        WorkflowTaskDO returnedTask = get(task.getId(), true);
        if (output.isSuspend()) {
            returnedTask.setTaskStatus(WorkflowTaskStateEnum.WAITING_SUSPEND.toString());
        } else {
            returnedTask.setTaskStatus(WorkflowTaskStateEnum.SUCCESS.toString());
        }
        returnedTask.setTaskErrorMessage("");
        if (output.getDeployAppId() != null && output.getDeployAppId() > 0) {
            returnedTask.setDeployAppId(output.getDeployAppId());
            returnedTask.setDeployAppUnitId(output.getDeployAppUnitId());
            returnedTask.setDeployAppNamespaceId(output.getDeployAppNamespaceId());
            returnedTask.setDeployAppStageId(output.getDeployAppStageId());
        }

        // 保存 Workflow 快照
        WorkflowSnapshotDO snapshot = workflowSnapshotService.update(UpdateWorkflowSnapshotReq.builder()
                .workflowTaskId(task.getId())
                .workflowInstanceId(task.getWorkflowInstanceId())
                .context(output.getContext())
                .configuration(output.getConfiguration())
                .build());
        log.info("workflow snapshot has updated|workflowInstanceId={}|workflowTaskId={}|workflowSnapshotId={}|" +
                        "context={}", snapshot.getWorkflowInstanceId(), snapshot.getWorkflowTaskId(), snapshot.getId(),
                JSONObject.toJSONString(output.getContext()));
        return returnedTask;
    }

    /**
     * 终止指定 Workflow 任务 (x -> TERMINATED)
     *
     * @param workflowTaskId WorkflowTaskID
     * @param extMessage     终止时的扩展信息存储字符串
     */
    @Override
    public boolean terminate(Long workflowTaskId, String extMessage) {
        return ExecuteWorkflowTaskWaitingObject.triggerTerminated(workflowTaskId, extMessage);
    }

    /**
     * 暂停指定 Workflow 任务 (RUNNING -> RUNNING_SUSPEND)
     *
     * @param workflowTaskId WorkflowTaskID
     * @param extMessage     暂停时的扩展信息存储字符串
     */
    @Override
    public void suspend(Long workflowTaskId, String extMessage) {
        ExecuteWorkflowTaskWaitingObject.triggerSuspend(workflowTaskId, extMessage);
    }

    /**
     * 标记指定 workflow task 状态 (异常情况，如 FAILURE/EXCEPTION/TERMINATED)
     *
     * @param workflowTaskId Workflow 任务 ID
     * @param status         状态
     * @param errorMessage   错误信息
     */
    private WorkflowTaskDO markAbnormalWorkflowTask(
            Long workflowTaskId, WorkflowTaskStateEnum status, String errorMessage) {
        WorkflowTaskDO task = get(workflowTaskId, true);
        task.setTaskStatus(status.toString());
        task.setTaskErrorMessage(errorMessage);
        return task;
    }
}
