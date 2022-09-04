package com.alibaba.tesla.appmanager.server.service.appoption.type;

import com.alibaba.tesla.appmanager.common.enums.AppOptionTypeEnum;
import com.alibaba.tesla.appmanager.server.event.loader.AppOptionTypeLoadedEvent;
import com.alibaba.tesla.appmanager.server.service.appoption.AppOptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component("IntegerAppOptionType")
public class IntegerAppOptionType implements AppOptionType {

    @Autowired
    private ApplicationEventPublisher publisher;

    /**
     * 初始化，注册自身
     */
    @PostConstruct
    public void init() {
        publisher.publishEvent(new AppOptionTypeLoadedEvent(
                this, AppOptionTypeEnum.INTEGER, this.getClass().getSimpleName()));
    }

    @Override
    public String encode(Object value) {
        return String.valueOf(value);
    }

    @Override
    public Object decode(String value) {
        if (value == null) {
            return 0L;
        }
        if ("true".equals(value)) {
            return 1L;
        } else if ("false".equals(value)) {
            return 0L;
        } else {
            return Long.valueOf(value);
        }
    }

    @Override
    public String defaultValue() {
        return "0";
    }
}
