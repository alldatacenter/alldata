package com.alibaba.tesla.appmanager.workflow.listener;

import com.alibaba.tesla.appmanager.common.enums.WorkflowInstanceEventEnum;
import com.alibaba.tesla.appmanager.common.enums.WorkflowInstanceStateEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.workflow.action.WorkflowInstanceStateAction;
import com.alibaba.tesla.appmanager.workflow.action.WorkflowInstanceStateActionManager;
import com.alibaba.tesla.appmanager.workflow.event.WorkflowInstanceEvent;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowInstanceDO;
import com.alibaba.tesla.appmanager.workflow.service.WorkflowInstanceService;
import com.google.common.base.Enums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Workflow Instance 事件监听器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component
public class WorkflowInstanceEventListener implements ApplicationListener<WorkflowInstanceEvent> {

    @Autowired
    private WorkflowInstanceStateActionManager workflowInstanceStateActionManager;

    @Autowired
    private WorkflowInstanceService workflowInstanceService;

    /**
     * 处理 App 部署单事件
     *
     * @param event 事件
     */
    @Async
    @Override
    public void onApplicationEvent(WorkflowInstanceEvent event) {
        Long workflowInstanceId = event.getInstance().getId();
        WorkflowInstanceEventEnum currentEvent = event.getEvent();
        String logPre = String.format("action=event.app.%s|message=", currentEvent.toString());
        WorkflowInstanceDO workflow = workflowInstanceService.get(workflowInstanceId, false);

        // 进行状态检测
        WorkflowInstanceStateEnum status = Enums.getIfPresent(WorkflowInstanceStateEnum.class,
                workflow.getWorkflowStatus()).orNull();
        if (status == null) {
            log.error(logPre + "invalid event, cannot identify current status|workflowInstanceId={}|" +
                    "status={}", workflowInstanceId, workflow.getWorkflowStatus());
            return;
        }
        WorkflowInstanceStateEnum nextStatus = status.next(currentEvent);
        if (nextStatus == null) {
            log.warn(logPre + "invalid event, cannot transform to next status|workflowInstanceId={}|" +
                    "status={}", workflowInstanceId, workflow.getWorkflowStatus());
            return;
        }

        // 状态转移
        workflow.setWorkflowStatus(nextStatus.toString());
        // maybe "", it's ok
        if (event.getInstance().getWorkflowErrorMessage() != null) {
            workflow.setWorkflowErrorMessage(event.getInstance().getWorkflowErrorMessage());
        }
        String logSuffix = String.format("|workflowInstanceId=%d|fromStatus=%s|toStatus=%s",
                workflowInstanceId, status, nextStatus);
        try {
            workflowInstanceService.update(workflow);
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
        WorkflowInstanceStateAction instance = workflowInstanceStateActionManager.getInstance(nextStatus.toString());
        try {
            workflow = workflowInstanceService.get(workflowInstanceId, true);
            instance.run(workflow);
        } catch (AppException e) {
            if (AppErrorCode.LOCKER_VERSION_EXPIRED.equals(e.getErrorCode())) {
                log.info(logPre + "locker version expired, skip" + logSuffix);
                return;
            }
            markAsException(workflowInstanceId, nextStatus, e.getErrorMessage());
        } catch (Exception e) {
            markAsException(workflowInstanceId, nextStatus, ExceptionUtils.getStackTrace(e));
        }
    }

    private void markAsException(Long workflowInstanceId, WorkflowInstanceStateEnum fromStatus, String errorMessage) {
        WorkflowInstanceDO workflow;
        workflow = workflowInstanceService.get(workflowInstanceId, false);
        workflow.setWorkflowStatus(WorkflowInstanceStateEnum.EXCEPTION.toString());
        workflow.setWorkflowErrorMessage(errorMessage);
        workflow.setGmtEnd(DateUtil.now());
        workflowInstanceService.update(workflow);
        log.warn("action=event.app.ERROR|message=status has changed|workflowInstanceId={}|fromStatus={}|" +
                        "toStatus={}|exception={}", workflowInstanceId, fromStatus.toString(),
                WorkflowInstanceStateEnum.EXCEPTION, errorMessage);
    }
}
