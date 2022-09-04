package com.alibaba.tesla.appmanager.server.service.appoption.type;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.enums.AppOptionTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.server.event.loader.AppOptionTypeLoadedEvent;
import com.alibaba.tesla.appmanager.server.service.appoption.AppOptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component("JsonAppOptionType")
public class JsonAppOptionType implements AppOptionType {

    @Autowired
    private ApplicationEventPublisher publisher;

    /**
     * 初始化，注册自身
     */
    @PostConstruct
    public void init() {
        publisher.publishEvent(new AppOptionTypeLoadedEvent(
                this, AppOptionTypeEnum.JSON, this.getClass().getSimpleName()));
    }

    @Override
    public String encode(Object value) {
        String result = JSONObject.toJSONString(value);
        if (!result.startsWith("{")) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "not json value: " + JSONObject.toJSONString(value));
        }
        return result;
    }

    @Override
    public Object decode(String value) {
        if (value == null) {
            return new JSONObject();
        }
        try {
            return JSONObject.parseObject(value);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "not json value: " + value);
        }
    }

    @Override
    public String defaultValue() {
        return "{}";
    }
}
