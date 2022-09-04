package com.alibaba.tesla.appmanager.server.job;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.appmanager.common.enums.DeployAppEventEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployAppStateEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployAppEvent;
import com.alibaba.tesla.appmanager.server.repository.condition.DeployAppQueryCondition;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import com.alibaba.tesla.appmanager.server.service.deploy.business.DeployAppBO;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 部署单状态查询保底 Job
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class DeploymentStatusSafetyJob {

    @Autowired
    private SystemProperties systemProperties;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 需要检查的等待状态的部署单
     */
    private static final List<DeployAppStateEnum> DEPLOY_APP_WAITING_STATUS_LIST = Arrays.asList(
            DeployAppStateEnum.CREATED,
            DeployAppStateEnum.BACKUP_CHECKING,
            DeployAppStateEnum.COMPONENT_CHECKING,
            DeployAppStateEnum.PROCESSING_COMPONENT,
            DeployAppStateEnum.WAITING_COMPONENT,
            DeployAppStateEnum.BACKING_UP,
            DeployAppStateEnum.PROCESSING,
            DeployAppStateEnum.WAITING
    );

    @Autowired
    private DeployAppService deployAppService;

    @Scheduled(cron = "${appmanager.cron-job.deploy-app-status-safety:-}")
    @SchedulerLock(name = "deploymentStatusSafetyJob")
    public void execute() {
        long limit = systemProperties.getDeploymentMaxRunningSeconds() * 1000;
        long currentTime = System.currentTimeMillis();
        for (DeployAppStateEnum status : DEPLOY_APP_WAITING_STATUS_LIST) {
            DeployAppQueryCondition condition = DeployAppQueryCondition.builder().deployStatus(status).build();
            Pagination<DeployAppBO> waitingList = deployAppService.list(condition, false);
            if (waitingList.isEmpty()) {
                continue;
            }
            log.info("action=deployAppStatusSafetyJob|get {} deployment orders with status {}",
                    waitingList.getTotal(), status.toString());
            for (DeployAppBO item : waitingList.getItems()) {
                long deployAppId = item.getOrder().getId();
                long orderTime = item.getOrder().getGmtCreate().getTime();
                if (orderTime + limit < currentTime) {
                    item.getOrder().setDeployErrorMessage("timeout, killed by system");
                    try {
                        deployAppService.update(item.getOrder());
                        DeployAppEvent event = new DeployAppEvent(this, DeployAppEventEnum.OP_TERMINATE, deployAppId);
                        eventPublisher.publishEvent(event);
                        log.error("action=deployAppStatusSafetyJob|found timeout deployment order, kill it|" +
                                        "deployAppId={}|orderTime={}|currentTime={}|order={}", deployAppId,
                                orderTime, currentTime, JSONObject.toJSONString(item.getOrder()));
                    } catch (AppException e) {
                        if (e.getErrorCode().equals(AppErrorCode.LOCKER_VERSION_EXPIRED)) {
                            log.info("action=deployAppStatusSafetyJob|lock failed on deployment order, skip it|" +
                                    "deployAppId={}", deployAppId);
                        } else {
                            log.error("action=deployAppStatusSafetyJob|cannot set error messages on " +
                                            "deployment order, skip|deployAppId={}|exception={}", deployAppId,
                                    ExceptionUtils.getStackTrace(e));
                        }
                    }
                }
            }
        }
    }
}
