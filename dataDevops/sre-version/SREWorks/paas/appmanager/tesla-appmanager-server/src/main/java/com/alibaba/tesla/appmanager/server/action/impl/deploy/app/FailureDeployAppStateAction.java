package com.alibaba.tesla.appmanager.server.action.impl.deploy.app;

import com.alibaba.tesla.appmanager.api.provider.WorkflowTaskProvider;
import com.alibaba.tesla.appmanager.common.enums.DeployAppStateEnum;
import com.alibaba.tesla.appmanager.common.enums.WorkflowTaskEventEnum;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.WorkflowTaskDTO;
import com.alibaba.tesla.appmanager.domain.req.workflow.WorkflowTaskListReq;
import com.alibaba.tesla.appmanager.server.action.DeployAppStateAction;
import com.alibaba.tesla.appmanager.server.event.loader.DeployAppStateActionLoadedEvent;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;
import com.alibaba.tesla.appmanager.workflow.event.WorkflowTaskEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * App 部署工单 State 处理 Action - FAILURE
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service("FailureDeployAppStateAction")
public class FailureDeployAppStateAction implements DeployAppStateAction, ApplicationRunner {

    private static final DeployAppStateEnum STATE = DeployAppStateEnum.FAILURE;

    private Timer timer;

    private Counter counter;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private WorkflowTaskProvider workflowTaskProvider;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        timer = meterRegistry.timer("deploy.app.status.failure.timer");
        counter = meterRegistry.counter("deploy.app.status.failure.counter");
        publisher.publishEvent(new DeployAppStateActionLoadedEvent(
                this, STATE.toString(), this.getClass().getSimpleName()));
    }

    /**
     * 自身逻辑处理
     *
     * @param order   部署工单
     * @param attrMap 部署属性字典
     */
    @Override
    public void run(DeployAppDO order, Map<String, String> attrMap) {
        String cost = order.costTime();
        if (StringUtils.isNumeric(cost)) {
            timer.record(Long.parseLong(cost), TimeUnit.MILLISECONDS);
        }
        counter.increment();
        log.info("deploy app has reached failure state|deployAppId={}|appPackageId={}|cost={}",
                order.getId(), order.getAppPackageId(), cost);

        // 如果 workflow 中存在关联项，那么发送事件，触发工作流继续进行
        WorkflowTaskListReq req = WorkflowTaskListReq.builder()
                .deployAppId(order.getId())
                .build();
        Pagination<WorkflowTaskDTO> workflowTasks = workflowTaskProvider.list(req);
        if (workflowTasks.getItems().size() == 0) {
            log.info("no associated workflow tasks found, skip|deployAppId={}", order.getId());
            return;
        }

        for (WorkflowTaskDTO item : workflowTasks.getItems()) {
            log.info("find associated workflow task, publish TRIGGER_UPDATE to it|workflowInstanceId={}|" +
                            "workflowTaskId={}|deployAppId={}|deployStatus={}", item.getWorkflowInstanceId(),
                    item.getId(), order.getId(), order.getDeployStatus());
            publisher.publishEvent(new WorkflowTaskEvent(this, WorkflowTaskEventEnum.TRIGGER_UPDATE, item));
        }
    }
}
