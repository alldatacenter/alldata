package com.alibaba.tesla.appmanager.server.event.componentpackage;

import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskEventEnum;
import org.springframework.context.ApplicationEvent;

/**
 * Deploy App 事件基类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class ComponentPackageTaskEvent extends ApplicationEvent {

    protected ComponentPackageTaskEventEnum CURRENT_EVENT;

    protected Long componentPackageTaskId;

    public ComponentPackageTaskEvent(Object source, Long componentPackageTaskId) {
        super(source);
        this.componentPackageTaskId = componentPackageTaskId;
    }

    public Long getComponentPackageTaskId() {
        return componentPackageTaskId;
    }

    public ComponentPackageTaskEventEnum getCurrentEvent() {
        return CURRENT_EVENT;
    }
}
