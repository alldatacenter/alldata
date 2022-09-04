package com.alibaba.tesla.appmanager.server.job;

import com.alibaba.tesla.appmanager.api.provider.WorkflowTaskProvider;
import com.alibaba.tesla.appmanager.common.enums.WorkflowTaskEventEnum;
import com.alibaba.tesla.appmanager.domain.dto.WorkflowTaskDTO;
import com.alibaba.tesla.appmanager.workflow.event.WorkflowTaskEvent;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Workflow 中远端 Deploy App 状态刷新 JOB
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class WorkflowRemoteDeployAppRefreshJob {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private WorkflowTaskProvider workflowTaskProvider;

    @Scheduled(cron = "${appmanager.cron-job.workflow-remote-deploy-app-refresh:0/10 * * * * *}")
    @SchedulerLock(name = "workflowRemoteDeployAppRefreshJob")
    public void execute() {
        List<WorkflowTaskDTO> tasks = workflowTaskProvider.listRunningRemoteTask();
        if (tasks.size() == 0) {
            return;
        }

        log.info("found {} running remote workflow tasks, prepare to trigger...", tasks.size());
        for (WorkflowTaskDTO task : tasks) {
            long deployAppId = task.getDeployAppId();
            String unitId = task.getDeployAppUnitId();
            String namespaceId = task.getDeployAppNamespaceId();
            String stageId = task.getDeployAppStageId();
            log.info("find running remote workflow task, publish TRIGGER_UPDATE to it|workflowInstanceId={}|" +
                            "workflowTaskId={}|deployAppId={}|deployAppUnitId={}|deployAppNamespaceId={}|" +
                            "deployAppStageId={}", task.getWorkflowInstanceId(), task.getId(), deployAppId,
                    unitId, namespaceId, stageId);
            publisher.publishEvent(new WorkflowTaskEvent(this, WorkflowTaskEventEnum.TRIGGER_UPDATE, task));
        }
    }
}
