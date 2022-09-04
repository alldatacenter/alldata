package com.alibaba.tesla.appmanager.server.listener;

import com.alibaba.tesla.appmanager.common.enums.DeployComponentEventEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployComponentStateEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.server.action.DeployComponentStateAction;
import com.alibaba.tesla.appmanager.server.action.DeployComponentStateActionManager;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployComponentEvent;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDO;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployComponentService;
import com.alibaba.tesla.appmanager.server.service.deploy.business.DeployComponentBO;
import com.google.common.base.Enums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Component 部署单开始事件监听器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component
public class DeployComponentEventListener implements ApplicationListener<DeployComponentEvent> {

    @Autowired
    private DeployComponentService deployComponentService;

    @Autowired
    private DeployComponentStateActionManager deployComponentStateActionManager;

    @Autowired
    private ApplicationEventPublisher publisher;

    /**
     * 处理 Component 部署单事件
     *
     * @param event 事件
     */
    @Async
    @Override
    public void onApplicationEvent(DeployComponentEvent event) {
        Long deployComponentId = event.getComponentDeployId();
        DeployComponentEventEnum currentEvent = event.getEvent();
        String logPre = String.format("action=event.component.%s|message=", currentEvent.toString());
        DeployComponentBO deployComponentBO = deployComponentService.get(deployComponentId, false);
        if (deployComponentBO == null) {
            log.error(logPre + "invalid event, cannot find component package in db|deployComponentId={}",
                    deployComponentId);
            return;
        }
        DeployComponentDO order = deployComponentBO.getSubOrder();
        Long deployAppId = order.getDeployId();

        // 进行状态检测
        DeployComponentStateEnum status = Enums
                .getIfPresent(DeployComponentStateEnum.class, order.getDeployStatus()).orNull();
        if (status == null) {
            log.error(logPre + "invalid event, cannot identify current status|deployAppId={}|deployComponentId={}|" +
                    "status={}", deployAppId, deployComponentId, order.getDeployStatus());
            return;
        }
        DeployComponentStateEnum nextStatus = status.next(currentEvent);
        if (nextStatus == null) {
            log.warn(logPre + "invalid event, cannot transform to next status|deployAppId={}|deployComponentId={}|" +
                    "status={}", deployAppId, deployComponentId, order.getDeployStatus());
            return;
        }

        // 状态转移
        order.setDeployStatus(nextStatus.toString());
        String logSuffix = String.format("|deployAppId=%d|deployComponentId=%d|fromStatus=%s|toStatus=%s",
                deployAppId, deployComponentId, status, nextStatus);
        try {
            deployComponentService.update(order);
        } catch (AppException e) {
            if (AppErrorCode.LOCKER_VERSION_EXPIRED.equals(e.getErrorCode())) {
                log.info(logPre + "locker version expired, skip" + logSuffix);
                return;
            }
        }
        log.info(logPre + "status has changed" + logSuffix);

        // 运行目标 State 的动作
        DeployComponentStateAction instance = deployComponentStateActionManager.getInstance(nextStatus.toString());
        try {
            deployComponentBO = deployComponentService.get(deployComponentId, true);
            instance.run(deployComponentBO.getSubOrder(), deployComponentBO.getAttrMap());
        } catch (AppException e) {
            if (AppErrorCode.LOCKER_VERSION_EXPIRED.equals(e.getErrorCode())) {
                log.info(logPre + "locker version expired, skip" + logSuffix);
                return;
            }
            markAsException(deployAppId, deployComponentId, nextStatus, e.getErrorMessage());
        } catch (Exception e) {
            markAsException(deployAppId, deployComponentId, nextStatus, ExceptionUtils.getStackTrace(e));
        }
    }

    private void markAsException(
            Long deployAppId, Long deployComponentId, DeployComponentStateEnum fromStatus, String errorMessage) {
        DeployComponentDO order;
        order = deployComponentService.get(deployComponentId, false).getSubOrder();
        order.setDeployStatus(DeployComponentStateEnum.EXCEPTION.toString());
        order.setDeployErrorMessage(errorMessage);
        deployComponentService.update(order);
        log.warn("action=event.component.ERROR|message=status has changed|deployAppId={}|deployComponentId={}|" +
                        "fromStatus={}|toStatus={}|exception={}", deployAppId, deployComponentId, fromStatus.toString(),
                DeployComponentStateEnum.EXCEPTION, errorMessage);
        publisher.publishEvent(new DeployComponentEvent(this, DeployComponentEventEnum.TRIGGER_UPDATE, order.getId()));
    }
}
