package com.alibaba.tesla.appmanager.workflow.listener;

import com.alibaba.tesla.appmanager.common.enums.WorkflowTaskEventEnum;
import com.alibaba.tesla.appmanager.common.enums.WorkflowTaskStateEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.workflow.action.WorkflowTaskStateAction;
import com.alibaba.tesla.appmanager.workflow.action.WorkflowTaskStateActionManager;
import com.alibaba.tesla.appmanager.workflow.event.WorkflowTaskEvent;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowTaskDO;
import com.alibaba.tesla.appmanager.workflow.service.WorkflowTaskService;
import com.google.common.base.Enums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Workflow Task 事件监听器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component
public class WorkflowTaskEventListener implements ApplicationListener<WorkflowTaskEvent> {

    @Autowired
    private WorkflowTaskStateActionManager workflowTaskStateActionManager;

    @Autowired
    private WorkflowTaskService workflowTaskService;

    /**
     * 处理 App 部署单事件
     *
     * @param event 事件
     */
    @Async
    @Override
    public void onApplicationEvent(WorkflowTaskEvent event) {
        Long workflowTaskId = event.getTask().getId();
        WorkflowTaskEventEnum currentEvent = event.getEvent();
        String logPre = String.format("action=event.app.%s|message=", currentEvent.toString());
        WorkflowTaskDO task = workflowTaskService.get(workflowTaskId, false);

        // 进行状态检测
        WorkflowTaskStateEnum status = Enums.getIfPresent(WorkflowTaskStateEnum.class,
                task.getTaskStatus()).orNull();
        if (status == null) {
            log.error(logPre + "invalid event, cannot identify current status|workflowTaskId={}|" +
                    "status={}", workflowTaskId, task.getTaskStatus());
            return;
        }
        WorkflowTaskStateEnum nextStatus = status.next(currentEvent);
        if (nextStatus == null) {
            log.warn(logPre + "invalid event, cannot transform to next status|workflowTaskId={}|" +
                    "status={}", workflowTaskId, task.getTaskStatus());
            return;
        }

        // 状态转移
        String logSuffix = String.format("|workflowTaskId=%d|fromStatus=%s|toStatus=%s|errorMessage=%s",
                workflowTaskId, status, nextStatus, event.getTask().getTaskErrorMessage());
        task.setTaskStatus(nextStatus.toString());
        // maybe "", it's ok
        if (event.getTask().getTaskErrorMessage() != null) {
            task.setTaskErrorMessage(event.getTask().getTaskErrorMessage());
        }
        if (event.getTask().getDeployAppId() != null && event.getTask().getDeployAppId() > 0) {
            task.setDeployAppId(event.getTask().getDeployAppId());
            task.setDeployAppUnitId(event.getTask().getDeployAppUnitId());
            task.setDeployAppNamespaceId(event.getTask().getDeployAppNamespaceId());
            task.setDeployAppStageId(event.getTask().getDeployAppStageId());
            logSuffix += String.format("|deployAppId=%d|deployAppUnitId=%s|deployAppNamespaceId=%s|deployAppStageId=%s",
                    event.getTask().getDeployAppId(), event.getTask().getDeployAppUnitId(),
                    event.getTask().getDeployAppNamespaceId(),
                    event.getTask().getDeployAppStageId());
        }
        try {
            workflowTaskService.update(task);
        } catch (AppException e) {
            if (AppErrorCode.LOCKER_VERSION_EXPIRED.equals(e.getErrorCode())) {
                log.info(logPre + "locker version expired, skip" + logSuffix);
                return;
            }
        }
        if (!status.toString().equals(nextStatus.toString())) {
            log.info(logPre + "status has changed" + logSuffix);
        }

        // 运行目标 State 的动作
        WorkflowTaskStateAction instance = workflowTaskStateActionManager.getInstance(nextStatus.toString());
        try {
            task = workflowTaskService.get(workflowTaskId, true);
            instance.run(task);
        } catch (AppException e) {
            if (AppErrorCode.LOCKER_VERSION_EXPIRED.equals(e.getErrorCode())) {
                log.info(logPre + "locker version expired, skip" + logSuffix);
                return;
            }
            markAsException(workflowTaskId, nextStatus, e.getErrorMessage());
        } catch (Exception e) {
            markAsException(workflowTaskId, nextStatus, ExceptionUtils.getStackTrace(e));
        }
    }

    private void markAsException(Long workflowTaskId, WorkflowTaskStateEnum fromStatus, String errorMessage) {
        WorkflowTaskDO workflow;
        workflow = workflowTaskService.get(workflowTaskId, false);
        workflow.setTaskStatus(WorkflowTaskStateEnum.EXCEPTION.toString());
        workflow.setTaskErrorMessage(errorMessage);
        workflow.setGmtEnd(DateUtil.now());
        workflowTaskService.update(workflow);
        log.warn("action=event.app.ERROR|message=status has changed|workflowTaskId={}|fromStatus={}|" +
                        "toStatus={}|exception={}", workflowTaskId, fromStatus.toString(),
                WorkflowTaskStateEnum.EXCEPTION, errorMessage);
    }
}
