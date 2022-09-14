package com.alibaba.tesla.appmanager.spring.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * 事件 - Application Context 属性加载完毕
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Getter
@Setter
public class ApplicationContextLoadedEvent extends ApplicationEvent {

    public ApplicationContextLoadedEvent(Object source) {
        super(source);
    }
}
