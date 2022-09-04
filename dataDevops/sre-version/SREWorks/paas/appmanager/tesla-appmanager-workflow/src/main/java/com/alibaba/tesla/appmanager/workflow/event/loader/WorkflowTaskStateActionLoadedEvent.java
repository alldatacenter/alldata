package com.alibaba.tesla.appmanager.workflow.event.loader;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * 事件 - Workflow Task 加载成功事件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Getter
@Setter
public class WorkflowTaskStateActionLoadedEvent extends ApplicationEvent {

    private String key;
    private String beanName;

    public WorkflowTaskStateActionLoadedEvent(Object source, String key, String beanName) {
        super(source);
        this.key = key;
        this.beanName = beanName;
    }
}
