package com.alibaba.tesla.appmanager.server.event.deploy;

import com.alibaba.tesla.appmanager.common.enums.DeployAppEventEnum;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Deploy App 事件基类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class DeployAppEvent extends ApplicationEvent {

    @Getter
    protected DeployAppEventEnum event;

    @Getter
    protected Long deployAppId;

    public DeployAppEvent(Object source, DeployAppEventEnum event, Long deployAppId) {
        super(source);
        this.event = event;
        this.deployAppId = deployAppId;
    }
}
