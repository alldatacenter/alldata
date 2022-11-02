package com.alibaba.tesla.appmanager.workflow.action.impl.workflowtask;

import com.alibaba.tesla.appmanager.common.enums.WorkflowTaskStateEnum;
import com.alibaba.tesla.appmanager.workflow.action.WorkflowTaskStateAction;
import com.alibaba.tesla.appmanager.workflow.event.loader.WorkflowTaskStateActionLoadedEvent;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowInstanceDO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowTaskDO;
import com.alibaba.tesla.appmanager.workflow.service.WorkflowInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service("FailureWorkflowTaskStateAction")
public class FailureWorkflowTaskStateAction implements WorkflowTaskStateAction, ApplicationRunner {

    private static final WorkflowTaskStateEnum STATE = WorkflowTaskStateEnum.FAILURE;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private WorkflowInstanceService workflowInstanceService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        publisher.publishEvent(new WorkflowTaskStateActionLoadedEvent(
                this, STATE.toString(), this.getClass().getSimpleName()));
    }

    /**
     * 自身处理逻辑
     *
     * @param task Workflow Task
     */
    @Override
    public void run(WorkflowTaskDO task) {
        log.info("the current workflow task enters the FAILURE state|workflowInstanceId={}|workflowTaskId={}",
                task.getWorkflowInstanceId(), task.getId());
        WorkflowInstanceDO instance = workflowInstanceService.get(task.getWorkflowInstanceId(), false);
        workflowInstanceService.triggerProcessFailed(instance, task.getTaskErrorMessage());
    }
}
