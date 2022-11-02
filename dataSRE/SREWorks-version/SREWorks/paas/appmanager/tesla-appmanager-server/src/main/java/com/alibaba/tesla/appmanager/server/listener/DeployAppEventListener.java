package com.alibaba.tesla.appmanager.server.listener;

import com.alibaba.tesla.appmanager.common.enums.DeployAppEventEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployAppStateEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.action.DeployAppStateAction;
import com.alibaba.tesla.appmanager.server.action.DeployAppStateActionManager;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployAppEvent;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import com.alibaba.tesla.appmanager.server.service.deploy.business.DeployAppBO;
import com.google.common.base.Enums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * App 部署单开始事件监听器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component
public class DeployAppEventListener implements ApplicationListener<DeployAppEvent> {

    @Autowired
    private DeployAppStateActionManager deployAppStateActionManager;

    @Autowired
    private DeployAppService deployAppService;

    @Autowired
    private ApplicationEventPublisher publisher;

    /**
     * 处理 App 部署单事件
     *
     * @param event 事件
     */
    @Async
    @Override
    public void onApplicationEvent(DeployAppEvent event) {
        Long deployAppId = event.getDeployAppId();
        DeployAppEventEnum currentEvent = event.getEvent();
        String logPre = String.format("action=event.app.%s|message=", currentEvent.toString());
        DeployAppBO deployAppBO = deployAppService.get(deployAppId, false);
        DeployAppDO order = deployAppBO.getOrder();

        // 进行状态检测
        DeployAppStateEnum status = Enums.getIfPresent(DeployAppStateEnum.class, order.getDeployStatus()).orNull();
        if (status == null) {
            log.error(logPre + "invalid event, cannot identify current status|deployAppId={}|" +
                    "status={}", deployAppId, order.getDeployStatus());
            return;
        }
        DeployAppStateEnum nextStatus = status.next(currentEvent);
        if (nextStatus == null) {
            log.warn(logPre + "invalid event, cannot transform to next status|deployAppId={}|" +
                    "status={}", deployAppId, order.getDeployStatus());
            return;
        }

        // 状态转移
        order.setDeployStatus(nextStatus.toString());
        String logSuffix = String.format("|deployAppId=%d|fromStatus=%s|toStatus=%s",
                deployAppId, status, nextStatus);
        try {
            deployAppService.update(order);
        } catch (AppException e) {
            if (AppErrorCode.LOCKER_VERSION_EXPIRED.equals(e.getErrorCode())) {
                log.info(logPre + "locker version expired, skip" + logSuffix);
                return;
            }
        }
        if (!status.toString().equals(nextStatus.toString())) {
            log.info(logPre + "status has changed" + logSuffix);
        }

        // 运行目标 State 的动作
        DeployAppStateAction instance = deployAppStateActionManager.getInstance(nextStatus.toString());
        try {
            deployAppBO = deployAppService.get(deployAppId, true);
            instance.run(deployAppBO.getOrder(), deployAppBO.getAttrMap());
        } catch (AppException e) {
            if (AppErrorCode.LOCKER_VERSION_EXPIRED.equals(e.getErrorCode())) {
                log.info(logPre + "locker version expired, skip" + logSuffix);
                return;
            }
            markAsException(deployAppId, nextStatus, e.getErrorMessage());
        } catch (Exception e) {
            markAsException(deployAppId, nextStatus, ExceptionUtils.getStackTrace(e));
        }
    }

    private void markAsException(Long deployAppId, DeployAppStateEnum fromStatus, String errorMessage) {
        DeployAppDO order;
        order = deployAppService.get(deployAppId, false).getOrder();
        order.setDeployStatus(DeployAppStateEnum.EXCEPTION.toString());
        order.setDeployErrorMessage(errorMessage);
        order.setGmtEnd(DateUtil.now());
        deployAppService.update(order);
        log.warn("action=event.app.ERROR|message=status has changed|deployAppId={}|fromStatus={}|" +
                        "toStatus={}|exception={}", deployAppId, fromStatus.toString(),
                DeployAppStateEnum.EXCEPTION, errorMessage);
        publisher.publishEvent(new DeployAppEvent(this, DeployAppEventEnum.TRIGGER_UPDATE, order.getId()));
    }
}
