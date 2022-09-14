package com.alibaba.tesla.appmanager.workflow.event.loader;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * 事件 - Workflow Instance 加载成功事件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Getter
@Setter
public class WorkflowInstanceStateActionLoadedEvent extends ApplicationEvent {

    private String key;
    private String beanName;

    public WorkflowInstanceStateActionLoadedEvent(Object source, String key, String beanName) {
        super(source);
        this.key = key;
        this.beanName = beanName;
    }
}
