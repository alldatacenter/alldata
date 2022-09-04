package com.alibaba.tesla.appmanager.server.service.appoption;

import com.alibaba.tesla.appmanager.common.enums.AppOptionTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.server.event.loader.AppOptionTypeLoadedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AppOptionType 管理器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class AppOptionTypeManager implements ApplicationListener<AppOptionTypeLoadedEvent> {

    @Autowired
    private ApplicationContext context;

    private final Map<String, AppOptionType> typeMap = new ConcurrentHashMap<>();

    /**
     * 获取 AppOptionType 处理器
     *
     * @param appOptionType 类型
     * @return Addon 对象，如果不存在则抛出异常
     */
    public AppOptionType get(String appOptionType) {
        AppOptionType result = this.typeMap.get(appOptionType);
        if (result == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find app option type by %s", appOptionType));
        }
        return result;
    }

    /**
     * 注册 AppOptionType
     *
     * @param key           Map Key
     * @param appOptionType 类型对象
     */
    private void register(AppOptionTypeEnum key, AppOptionType appOptionType) {
        typeMap.put(key.toString(), appOptionType);
    }

    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(AppOptionTypeLoadedEvent event) {
        AppOptionTypeEnum key = event.getKey();
        Object bean;
        try {
            bean = context.getBean(event.getBeanName());
        } catch (Exception e) {
            String message = String.format("cannot get bean now, failed to load app option type|beanName=%s|key=%s",
                    event.getBeanName(), event.getKey());
            log.error(message);
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, message);
        }
        register(key, (AppOptionType) bean);
        log.info("app option type {} has registered", key);
    }
}
