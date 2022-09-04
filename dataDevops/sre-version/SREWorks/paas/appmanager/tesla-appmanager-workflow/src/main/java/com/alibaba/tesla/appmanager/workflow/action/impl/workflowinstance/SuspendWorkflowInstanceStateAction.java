package com.alibaba.tesla.appmanager.workflow.action.impl.workflowinstance;

import com.alibaba.tesla.appmanager.common.enums.WorkflowInstanceStateEnum;
import com.alibaba.tesla.appmanager.workflow.action.WorkflowInstanceStateAction;
import com.alibaba.tesla.appmanager.workflow.event.loader.WorkflowInstanceStateActionLoadedEvent;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowInstanceDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service("SuspendWorkflowInstanceStateAction")
public class SuspendWorkflowInstanceStateAction implements WorkflowInstanceStateAction, ApplicationRunner {

    private static final WorkflowInstanceStateEnum STATE = WorkflowInstanceStateEnum.SUSPEND;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        publisher.publishEvent(new WorkflowInstanceStateActionLoadedEvent(
                this, STATE.toString(), this.getClass().getSimpleName()));
    }

    /**
     * 自身处理逻辑
     *
     * @param instance Workflow 实例
     */
    @Override
    public void run(WorkflowInstanceDO instance) {
        log.info("the current workflow instance has entered the SUSPEND state|workflowInstanceId={}|appId={}",
                instance.getId(), instance.getAppId());
    }
}
