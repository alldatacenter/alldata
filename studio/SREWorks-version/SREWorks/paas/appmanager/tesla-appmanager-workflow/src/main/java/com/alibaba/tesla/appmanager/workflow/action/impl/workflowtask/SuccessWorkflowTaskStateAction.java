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
@Service("SuccessWorkflowTaskStateAction")
public class SuccessWorkflowTaskStateAction implements WorkflowTaskStateAction, ApplicationRunner {

    private static final WorkflowTaskStateEnum STATE = WorkflowTaskStateEnum.SUCCESS;

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
     * @param task Workflow 实例
     */
    @Override
    public void run(WorkflowTaskDO task) {
        log.info("the current workflow task enters the SUCCESS state|workflowInstanceId={}|workflowTaskId={}",
                task.getWorkflowInstanceId(), task.getId());

        // 触发当前 Workflow Task 对应的 Workflow Instance 中下一个 Workflow Task 的运行
        WorkflowInstanceDO instance = workflowInstanceService.get(task.getWorkflowInstanceId(), true);
        workflowInstanceService.triggerNextPendingTask(instance, task.getId());
        log.info("trigger workflow instance next task success|workflowInstanceId={}|previousTaskId={}",
                task.getId(), task.getId());
    }
}
