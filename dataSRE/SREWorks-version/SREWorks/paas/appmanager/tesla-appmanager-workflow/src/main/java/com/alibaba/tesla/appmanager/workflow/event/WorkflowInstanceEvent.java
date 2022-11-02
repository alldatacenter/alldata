package com.alibaba.tesla.appmanager.workflow.event;

import com.alibaba.tesla.appmanager.common.enums.WorkflowInstanceEventEnum;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowInstanceDO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Workflow Instance 事件基类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class WorkflowInstanceEvent extends ApplicationEvent {

    @Getter
    protected WorkflowInstanceEventEnum event;

    @Getter
    protected WorkflowInstanceDO instance;

    public WorkflowInstanceEvent(Object source, WorkflowInstanceEventEnum event, WorkflowInstanceDO instance) {
        super(source);
        this.event = event;
        this.instance = instance;
    }
}
