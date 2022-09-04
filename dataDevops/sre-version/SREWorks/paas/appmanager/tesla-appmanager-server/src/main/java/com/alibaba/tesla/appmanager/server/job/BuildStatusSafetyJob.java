package com.alibaba.tesla.appmanager.server.job;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskStateEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployAppEventEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployAppStateEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.server.event.componentpackage.ComponentPackageTaskEvent;
import com.alibaba.tesla.appmanager.server.event.componentpackage.FailedComponentPackageTaskEvent;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployAppEvent;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.DeployAppQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;
import com.alibaba.tesla.appmanager.server.service.componentpackage.ComponentPackageTaskService;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import com.alibaba.tesla.appmanager.server.service.deploy.business.DeployAppBO;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 构建任务状态查询保底 Job
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class BuildStatusSafetyJob {

    private final SystemProperties systemProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final ComponentPackageTaskService componentPackageTaskService;

    /**
     * 需要检查的等待状态的构建任务
     */
    private static final List<ComponentPackageTaskStateEnum> COMPONENT_PACKAGE_TASK_WAITING_STATUS_LIST = Arrays.asList(
            ComponentPackageTaskStateEnum.CREATED,
            ComponentPackageTaskStateEnum.RUNNING
    );

    public BuildStatusSafetyJob(
            SystemProperties systemProperties, ApplicationEventPublisher eventPublisher,
            ComponentPackageTaskService componentPackageTaskService) {
        this.systemProperties = systemProperties;
        this.eventPublisher = eventPublisher;
        this.componentPackageTaskService = componentPackageTaskService;
    }

    @Scheduled(cron = "${appmanager.cron-job.build-status-safety:-}")
    @SchedulerLock(name = "buildStatusSafetyJob")
    public void execute() {
        long limit = systemProperties.getBuildMaxRunningSeconds() * 1000;
        long currentTime = System.currentTimeMillis();
        for (ComponentPackageTaskStateEnum status : COMPONENT_PACKAGE_TASK_WAITING_STATUS_LIST) {
            ComponentPackageTaskQueryCondition condition = ComponentPackageTaskQueryCondition.builder()
                    .taskStatus(status.toString())
                    .build();
            Pagination<ComponentPackageTaskDO> waitingList = componentPackageTaskService.list(condition);
            if (waitingList.isEmpty()) {
                continue;
            }
            log.info("action=buildStatusSafetyJob|get {} component package tasks with status {}",
                    waitingList.getTotal(), status);
            for (ComponentPackageTaskDO item : waitingList.getItems()) {
                long taskId = item.getId();
                long orderTime = item.getGmtCreate().getTime();
                if (orderTime + limit < currentTime) {
                    ComponentPackageTaskQueryCondition modifiedCondition = ComponentPackageTaskQueryCondition.builder()
                            .id(item.getId())
                            .withBlobs(true)
                            .build();
                    ComponentPackageTaskDO modifiedTask = componentPackageTaskService.get(modifiedCondition);
                    String errorMessage = "timeout, killed by system";
                    String taskLog = modifiedTask.getTaskLog();
                    if (StringUtils.isEmpty(taskLog)) {
                        taskLog = errorMessage;
                    } else {
                        taskLog += "\n" + errorMessage;
                    }
                    modifiedTask.setTaskLog(taskLog);
                    try {
                        componentPackageTaskService.update(modifiedTask, modifiedCondition);
                        FailedComponentPackageTaskEvent event = new FailedComponentPackageTaskEvent(this, taskId);
                        eventPublisher.publishEvent(event);
                        log.warn("action=buildStatusSafetyJob|found timeout component package task order, kill it|" +
                                        "taskId={}|orderTime={}|currentTime={}|task={}", taskId,
                                orderTime, currentTime, JSONObject.toJSONString(item));
                    } catch (AppException e) {
                        if (e.getErrorCode().equals(AppErrorCode.LOCKER_VERSION_EXPIRED)) {
                            log.info("action=buildStatusSafetyJob|lock failed on component package task, skip it|" +
                                    "taskId={}", taskId);
                        } else {
                            log.error("action=buildStatusSafetyJob|cannot set error messages on " +
                                            "component package task, skip|taskId={}|exception={}", taskId,
                                    ExceptionUtils.getStackTrace(e));
                        }
                    }
                }
            }
        }
    }
}
