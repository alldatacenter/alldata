package com.alibaba.tesla.appmanager.server.event.loader;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * 事件 - ComponentPackage ACTIOn 处理逻辑加载成功事件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Getter
@Setter
public class ComponentPackageTaskStateActionLoadedEvent extends ApplicationEvent {

    private String key;
    private String beanName;

    public ComponentPackageTaskStateActionLoadedEvent(Object source, String key, String beanName) {
        super(source);
        this.key = key;
        this.beanName = beanName;
    }
}
