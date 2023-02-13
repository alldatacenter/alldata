package com.alibaba.tesla.appmanager.workflow.event;

import com.alibaba.tesla.appmanager.common.enums.WorkflowTaskEventEnum;
import com.alibaba.tesla.appmanager.domain.dto.WorkflowTaskDTO;
import com.alibaba.tesla.appmanager.workflow.assembly.WorkflowTaskDtoConvert;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowTaskDO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Workflow Task 事件基类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class WorkflowTaskEvent extends ApplicationEvent {

    @Getter
    protected WorkflowTaskEventEnum event;

    @Getter
    protected WorkflowTaskDO task;

    public WorkflowTaskEvent(Object source, WorkflowTaskEventEnum event, WorkflowTaskDO task) {
        super(source);
        this.event = event;
        this.task = task;
    }

    public WorkflowTaskEvent(Object source, WorkflowTaskEventEnum event, WorkflowTaskDTO task) {
        super(source);
        this.event = event;
        this.task = (new WorkflowTaskDtoConvert()).from(task);
    }
}
