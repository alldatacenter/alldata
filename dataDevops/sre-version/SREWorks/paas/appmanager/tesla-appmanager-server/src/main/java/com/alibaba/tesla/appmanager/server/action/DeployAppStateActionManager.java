package com.alibaba.tesla.appmanager.server.action;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.server.event.loader.DeployAppStateActionLoadedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * App 部署工单 State Action 管理器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class DeployAppStateActionManager implements ApplicationListener<DeployAppStateActionLoadedEvent> {

    private static final String LOG_PRE = "[" + DeployAppStateActionManager.class.getSimpleName()
            + "] action=actionManager.deployApp|message=";

    @Autowired
    private ApplicationContext context;

    private ConcurrentMap<String, DeployAppStateAction> instanceMap = new ConcurrentHashMap<>();

    /**
     * 注册 State 处理 Action
     *
     * @param key      State 名称
     * @param instance 实例
     */
    private void register(String key, DeployAppStateAction instance) {
        instanceMap.put(key, instance);
    }

    /**
     * 自动根据事件注册 Instance 实例
     *
     * @param event 事件
     */
    @Override
    public void onApplicationEvent(DeployAppStateActionLoadedEvent event) {
        String key = event.getKey();
        Object bean;
        try {
            bean = context.getBean(event.getBeanName());
        } catch (Exception e) {
            String message = String.format("cannot get bean now, failed to load action instance|beanName=%s|key=%s",
                    event.getBeanName(), event.getKey());
            log.error(message);
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, message);
        }
        register(key, (DeployAppStateAction) bean);
        log.info("deploy app state action instance {} has registered", key);
    }

    /**
     * 获取指定 State 的实例
     *
     * @param key State 名称
     * @return DeployAppStateAction 实例
     */
    public DeployAppStateAction getInstance(String key) {
        DeployAppStateAction instance = instanceMap.get(key);
        if (instance == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid state name " + key);
        }
        return instance;
    }
}
