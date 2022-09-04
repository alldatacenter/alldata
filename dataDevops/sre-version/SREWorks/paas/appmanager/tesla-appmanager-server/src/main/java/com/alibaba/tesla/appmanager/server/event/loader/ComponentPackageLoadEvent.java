package com.alibaba.tesla.appmanager.server.event.loader;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * @ClassName: ComponentPackageLoadEvent
 * @Author: dyj
 * @DATE: 2021-03-09
 * @Description:
 **/
@Getter
@Setter
public class ComponentPackageLoadEvent extends ApplicationEvent {
    private String key;
    private String beanName;

    public ComponentPackageLoadEvent(Object source) {
        super(source);
    }

    public ComponentPackageLoadEvent(Object source, String key, String beanName) {
        super(source);
        this.key = key;
        this.beanName = beanName;
    }
}
