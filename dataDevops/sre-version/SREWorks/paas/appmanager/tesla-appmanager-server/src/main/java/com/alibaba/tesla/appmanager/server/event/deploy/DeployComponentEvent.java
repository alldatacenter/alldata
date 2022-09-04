package com.alibaba.tesla.appmanager.server.event.deploy;

import com.alibaba.tesla.appmanager.common.enums.DeployComponentEventEnum;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Deploy Component 事件基类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class DeployComponentEvent extends ApplicationEvent {

    @Getter
    protected DeployComponentEventEnum event;

    @Getter
    protected Long componentDeployId;

    public DeployComponentEvent(Object source, DeployComponentEventEnum event, Long componentDeployId) {
        super(source);
        this.event = event;
        this.componentDeployId = componentDeployId;
    }
}
