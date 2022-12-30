package com.alibaba.tesla.appmanager.server.action;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.server.event.loader.DeployComponentStateActionLoadedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Component 部署工单 State Action 管理器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class DeployComponentStateActionManager implements ApplicationListener<DeployComponentStateActionLoadedEvent> {

    private static final String LOG_PRE = "[" + DeployComponentStateActionManager.class.getSimpleName()
            + "] action=actionManager.deployComponent|message=";

    @Autowired
    private ApplicationContext context;

    private ConcurrentMap<String, DeployComponentStateAction> instanceMap = new ConcurrentHashMap<>();

    /**
     * 注册 State 处理 Action
     *
     * @param key      State 名称
     * @param instance 实例
     */
    private void register(String key, DeployComponentStateAction instance) {
        instanceMap.put(key, instance);
    }

    /**
     * 自动根据事件注册 Instance 实例
     *
     * @param event 事件
     */
    @Override
    public void onApplicationEvent(DeployComponentStateActionLoadedEvent event) {
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
        register(key, (DeployComponentStateAction) bean);
        log.info("deploy component state action instance {} has registered", key);
    }

    /**
     * 获取指定 State 的实例
     *
     * @param key State 名称
     * @return DeployComponentStateAction 实例
     */
    public DeployComponentStateAction getInstance(String key) {
        DeployComponentStateAction instance = instanceMap.get(key);
        if (instance == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid state name " + key);
        }
        return instance;
    }
}
