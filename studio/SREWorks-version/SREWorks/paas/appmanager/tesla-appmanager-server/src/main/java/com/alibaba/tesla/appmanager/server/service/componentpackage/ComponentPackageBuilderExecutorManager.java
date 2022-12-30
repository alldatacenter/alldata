package com.alibaba.tesla.appmanager.server.service.componentpackage;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.server.event.loader.ComponentPackageLoadEvent;
import com.alibaba.tesla.appmanager.server.service.componentpackage.instance.ComponentPackageBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @ClassName: ComponentPackageManager
 * @Author: dyj
 * @DATE: 2021-03-09
 * @Description:
 **/
@Service
@Slf4j
public class ComponentPackageBuilderExecutorManager implements ApplicationListener<ComponentPackageLoadEvent> {
    @Autowired
    private ApplicationContext context;

    private ConcurrentMap<String, ComponentPackageBase> instanceMap = new ConcurrentHashMap<>();

    private void register(String key, ComponentPackageBase componentPackageBase) {
        instanceMap.put(key, componentPackageBase);
    }

    @Override
    public void onApplicationEvent(ComponentPackageLoadEvent event) {
        String key = event.getKey();
        Object bean;
        try {
            bean = context.getBean(event.getBeanName());
        } catch (Exception e) {
            String message = String.format("cannot get bean now, failed to load componentPackageBase instance||beanName=%s||key=%s",
                    event.getBeanName(), event.getKey());
            log.error(message);
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, message);
        }
        register(key, (ComponentPackageBase) bean);
        log.info("deploy app componentPackageBase instance {} has registered", key);
    }

    public ComponentPackageBase getInstance(String key) {
        ComponentPackageBase instance = instanceMap.get(key);
        if (instance == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid state name " + key);
        }
        return instance;
    }
}
