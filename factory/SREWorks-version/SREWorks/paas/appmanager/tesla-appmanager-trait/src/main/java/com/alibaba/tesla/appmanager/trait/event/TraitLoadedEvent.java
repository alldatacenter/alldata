package com.alibaba.tesla.appmanager.trait.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * 事件 - Trait 加载成功事件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Getter
@Setter
public class TraitLoadedEvent extends ApplicationEvent {

    private String key;
    private String beanName;

    public TraitLoadedEvent(Object source, String key, String beanName) {
        super(source);
        this.key = key;
        this.beanName = beanName;
    }
}
