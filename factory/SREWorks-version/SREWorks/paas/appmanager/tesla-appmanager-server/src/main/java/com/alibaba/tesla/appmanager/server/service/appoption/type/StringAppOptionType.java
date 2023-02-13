package com.alibaba.tesla.appmanager.server.service.appoption.type;

import com.alibaba.tesla.appmanager.common.enums.AppOptionTypeEnum;
import com.alibaba.tesla.appmanager.server.event.loader.AppOptionTypeLoadedEvent;
import com.alibaba.tesla.appmanager.server.service.appoption.AppOptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component("StringAppOptionType")
public class StringAppOptionType implements AppOptionType {

    @Autowired
    private ApplicationEventPublisher publisher;

    /**
     * 初始化，注册自身
     */
    @PostConstruct
    public void init() {
        publisher.publishEvent(new AppOptionTypeLoadedEvent(
                this, AppOptionTypeEnum.STRING, this.getClass().getSimpleName()));
    }

    @Override
    public String encode(Object value) {
        return String.valueOf(value);
    }

    @Override
    public Object decode(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    @Override
    public String defaultValue() {
        return "";
    }
}
