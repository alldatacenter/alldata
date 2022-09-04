package com.alibaba.tesla.appmanager.server.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.tesla.appmanager.server.event.componentpackage.ComponentPackageTaskStartEvent;
import com.alibaba.tesla.appmanager.server.service.pack.PackService;
import com.alibaba.tesla.appmanager.domain.container.ComponentPackageTaskMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 组件包任务启动事件处理器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component
public class ComponentPackageTaskStartEventListener implements ApplicationListener<ComponentPackageTaskStartEvent> {

    @Autowired
    private PackService packService;

    @Async
    @Override
    public void onApplicationEvent(ComponentPackageTaskStartEvent event) {
        ComponentPackageTaskMessage message = ComponentPackageTaskMessage.builder()
                .appPackageTaskId(event.getAppPackageTaskId())
                .componentPackageTaskId(event.getComponentPackageTaskId())
                .appId(event.getAppId())
                .namespaceId(event.getNamespaceId())
                .stageId(event.getStageId())
                .operator(event.getOperator())
                .component(event.getComponent())
                .packageTaskEnum(event.getPackageTaskEnum())
                .build();
        log.info("action=taskConsumer|listenComponetPackageTask|componetPackageTask={}",
                JSON.toJSONString(message));
        try {
            switch (message.getPackageTaskEnum()) {
                case CREATE:
                    packService.createComponentPackageTask(message);
                    break;
                case RETRY:
                    packService.retryComponentPackageTask(message);
                    break;
            }
        } catch (Exception e) {
            log.error("action=taskConsumer|listenComponetPackageTask|exception={}", ExceptionUtils.getStackTrace(e));
        }
    }
}
