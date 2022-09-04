package com.alibaba.tesla.appmanager.server.listener;

import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskEventEnum;
import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskStateEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployAppStateEnum;
import com.alibaba.tesla.appmanager.server.action.ComponentPackageTaskStateAction;
import com.alibaba.tesla.appmanager.server.action.ComponentPackageTaskStateActionManager;
import com.alibaba.tesla.appmanager.server.event.componentpackage.ComponentPackageTaskEvent;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;
import com.google.common.base.Enums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Component Package 任务事件监听器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component
public class ComponentPackageTaskEventListener implements ApplicationListener<ComponentPackageTaskEvent> {

    private static final String LOG_PRE = "[event.create-component-package] ";

    @Autowired
    private ComponentPackageTaskRepository componentPackageTaskRepository;

    @Autowired
    private ComponentPackageTaskStateActionManager componentPackageTaskStateActionManager;

    @Autowired
    private ApplicationEventPublisher publisher;

    /**
     * 处理 App 部署单事件
     *
     * @param event 事件
     */
    @Async
    @Override
    public void onApplicationEvent(ComponentPackageTaskEvent event) {
        Long componentPackageTaskId = event.getComponentPackageTaskId();
        ComponentPackageTaskEventEnum currentEvent = event.getCurrentEvent();
        ComponentPackageTaskQueryCondition condition = ComponentPackageTaskQueryCondition.builder()
                .id(componentPackageTaskId)
                .build();
        ComponentPackageTaskDO task = componentPackageTaskRepository.getByCondition(condition);
        if (task == null) {
            log.error(LOG_PRE + "invalid event, cannot find component package task in db|componentPackageTaskId={}",
                    componentPackageTaskId);
            return;
        }
        String logSuffix = String.format("appId=%s|componentType=%s|componentName=%s|version=%s|operator=%s|" +
                        "componentPackageTaskId=%d", task.getAppId(), task.getComponentType(), task.getComponentName(),
                task.getPackageVersion(), task.getPackageCreator(), componentPackageTaskId);

        // 进行状态检测
        ComponentPackageTaskStateEnum status = Enums
                .getIfPresent(ComponentPackageTaskStateEnum.class, task.getTaskStatus()).orNull();
        if (status == null) {
            log.error(LOG_PRE + "invalid event, cannot identify current status|status={}|{}",
                    task.getTaskStatus(), logSuffix);
            return;
        }
        ComponentPackageTaskStateEnum nextStatus = status.next(currentEvent);
        if (nextStatus == null) {
            log.warn(LOG_PRE + "invalid event, cannot transform to next status|status={}|{}",
                    task.getTaskStatus(), logSuffix);
            return;
        }

        // 运行目标 State 的动作
        ComponentPackageTaskStateAction instance = componentPackageTaskStateActionManager
                .getInstance(nextStatus.toString());
        try {
            task = componentPackageTaskRepository.getByCondition(condition);
            instance.run(task);
        } catch (Exception e) {
            String errorMessage = ExceptionUtils.getStackTrace(e);
            task = componentPackageTaskRepository.getByCondition(condition);
            task.setTaskStatus(ComponentPackageTaskStateEnum.FAILURE.toString());
            task.setTaskLog(errorMessage);
            componentPackageTaskRepository.updateByCondition(task, ComponentPackageTaskQueryCondition.builder()
                    .id(task.getId())
                    .build());
            log.warn(LOG_PRE + "status transitioned from {} to {}|{}|exception={}", nextStatus,
                    DeployAppStateEnum.EXCEPTION, logSuffix, errorMessage);
        }
    }
}
