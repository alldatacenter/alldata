package com.alibaba.tesla.appmanager.server.event.loader;

import com.alibaba.tesla.appmanager.common.enums.AppOptionTypeEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * 事件 - AppOptionType 加载成功事件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Getter
@Setter
public class AppOptionTypeLoadedEvent extends ApplicationEvent {

    private AppOptionTypeEnum key;
    private String beanName;

    public AppOptionTypeLoadedEvent(Object source, AppOptionTypeEnum key, String beanName) {
        super(source);
        this.key = key;
        this.beanName = beanName;
    }
}
