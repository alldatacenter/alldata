package com.alibaba.tesla.appmanager.workflow.action.impl.workflowtask;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.enums.WorkflowTaskEventEnum;
import com.alibaba.tesla.appmanager.common.enums.WorkflowTaskStateEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.workflow.action.WorkflowTaskStateAction;
import com.alibaba.tesla.appmanager.workflow.event.WorkflowTaskEvent;
import com.alibaba.tesla.appmanager.workflow.event.loader.WorkflowTaskStateActionLoadedEvent;
import com.alibaba.tesla.appmanager.workflow.repository.condition.WorkflowSnapshotQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowInstanceDO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowSnapshotDO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowTaskDO;
import com.alibaba.tesla.appmanager.workflow.service.WorkflowInstanceService;
import com.alibaba.tesla.appmanager.workflow.service.WorkflowSnapshotService;
import com.alibaba.tesla.appmanager.workflow.service.WorkflowTaskService;
import com.google.common.base.Enums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service("RunningWorkflowTaskStateAction")
public class RunningWorkflowTaskStateAction implements WorkflowTaskStateAction, ApplicationRunner {

    private static final WorkflowTaskStateEnum STATE = WorkflowTaskStateEnum.RUNNING;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private WorkflowTaskService workflowTaskService;

    @Autowired
    private WorkflowInstanceService workflowInstanceService;

    @Autowired
    private WorkflowSnapshotService workflowSnapshotService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        publisher.publishEvent(new WorkflowTaskStateActionLoadedEvent(
                this, STATE.toString(), this.getClass().getSimpleName()));
    }

    /**
     * 自身处理逻辑
     *
     * @param task Workflow 实例
     */
    @Override
    public void run(WorkflowTaskDO task) {
        WorkflowInstanceDO instance = workflowInstanceService.get(task.getWorkflowInstanceId(), true);
        if (instance == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find related workflow instance by id %d|workflowTaskId=%d",
                            task.getWorkflowInstanceId(), task.getId()));
        }
        Pagination<WorkflowSnapshotDO> snapshots = workflowSnapshotService
                .list(WorkflowSnapshotQueryCondition.builder()
                        .instanceId(task.getWorkflowInstanceId())
                        .taskId(task.getId())
                        .build());
        if (snapshots.getItems().size() != 1) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find related workflow snapshot|workflowInstanceId=%d|workflowTaskId=%d|" +
                            "size=%d", task.getWorkflowInstanceId(), task.getId(), snapshots.getItems().size()));
        }
        String snapshotContextStr = snapshots.getItems().get(0).getSnapshotContext();
        if (StringUtils.isEmpty(snapshotContextStr)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("empty workflow snapshot context|workflowInstanceId=%d|workflowTaskId=%d",
                            task.getWorkflowInstanceId(), task.getId()));
        }
        JSONObject context = JSONObject.parseObject(snapshotContextStr);
        WorkflowTaskDO res = workflowTaskService.execute(instance, task, context);
        WorkflowTaskStateEnum status = Enums.getIfPresent(WorkflowTaskStateEnum.class, res.getTaskStatus()).orNull();
        if (status == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("invalid workflow task status|workflowInstanceId=%d|taskStatus=%s",
                            res.getId(), res.getTaskStatus()));
        }
        switch (status) {
            case SUCCESS:
                publisher.publishEvent(new WorkflowTaskEvent(this, WorkflowTaskEventEnum.PROCESS_FINISHED, res));
                break;
            case FAILURE:
                publisher.publishEvent(new WorkflowTaskEvent(this, WorkflowTaskEventEnum.PROCESS_FAILED, res));
                break;
            case EXCEPTION:
                publisher.publishEvent(new WorkflowTaskEvent(this, WorkflowTaskEventEnum.PROCESS_UNKNOWN_ERROR, res));
                break;
            case TERMINATED:
                publisher.publishEvent(new WorkflowTaskEvent(this, WorkflowTaskEventEnum.OP_TERMINATE, res));
                break;
            case RUNNING_SUSPEND:
                publisher.publishEvent(new WorkflowTaskEvent(this, WorkflowTaskEventEnum.PAUSE, res));
                break;
            case WAITING_SUSPEND:
                publisher.publishEvent(new WorkflowTaskEvent(this, WorkflowTaskEventEnum.PROCESS_SUSPEND, res));
                break;
            default:
                String errorMessage = String.format("invalid workflow task status found in " +
                        "RunningWorkflowTaskStateAction|workflowInstanceId=%d|status=%s", res.getId(), status);
                log.error(errorMessage);
                throw new AppException(AppErrorCode.INVALID_USER_ARGS, errorMessage);
        }
    }
}
