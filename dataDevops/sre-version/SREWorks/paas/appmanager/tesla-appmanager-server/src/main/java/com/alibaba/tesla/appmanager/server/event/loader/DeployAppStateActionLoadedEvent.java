package com.alibaba.tesla.appmanager.server.event.loader;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * 事件 - App 部署工单加载成功事件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Getter
@Setter
public class DeployAppStateActionLoadedEvent extends ApplicationEvent {

    private String key;
    private String beanName;

    public DeployAppStateActionLoadedEvent(Object source, String key, String beanName) {
        super(source);
        this.key = key;
        this.beanName = beanName;
    }
}
