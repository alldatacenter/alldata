package com.alibaba.tesla.appmanager.server.job;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.enums.AppPackageTaskStatusEnum;
import com.alibaba.tesla.appmanager.common.enums.ProductReleaseTaskStatusEnum;
import com.alibaba.tesla.appmanager.domain.req.productrelease.ListProductReleaseTaskAppPackageTaskReq;
import com.alibaba.tesla.appmanager.domain.req.productrelease.ListProductReleaseTaskReq;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageTaskInQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskAppPackageTaskRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskDO;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageTaskService;
import com.alibaba.tesla.appmanager.server.service.productrelease.ProductReleaseService;
import com.google.common.base.Enums;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 产品发布版本任务状态更新 Job
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class ProductReleaseTaskStatusUpdateJob {

    @Autowired
    private ProductReleaseService productReleaseService;

    @Autowired
    private AppPackageTaskService appPackageTaskService;

    @Scheduled(cron = "${appmanager.cron-job.product-release-task-status-update:0/20 * * * * *}")
    @SchedulerLock(name = "productReleaseTaskStatusUpdateJob")
    public void run() {
        ListProductReleaseTaskReq request = ListProductReleaseTaskReq.builder()
                .status(Collections.singletonList(ProductReleaseTaskStatusEnum.RUNNING.toString()))
                .build();
        List<ProductReleaseTaskDO> tasks = productReleaseService.listProductReleaseTask(request);
        if (tasks.size() == 0) {
            return;
        }

        log.info("get {} running product release tasks from database", tasks.size());
        for (ProductReleaseTaskDO task : tasks) {
            String taskId = task.getTaskId();
            List<ProductReleaseTaskAppPackageTaskRelDO> rels = productReleaseService
                    .listProductReleaseTaskAppPackageTask(ListProductReleaseTaskAppPackageTaskReq.builder()
                            .taskId(taskId)
                            .build());
            List<AppPackageTaskDO> appPackageTasks = appPackageTaskService.listIn(
                    AppPackageTaskInQueryCondition.builder()
                            .idList(rels.stream()
                                    .map(ProductReleaseTaskAppPackageTaskRelDO::getAppPackageTaskId)
                                    .collect(Collectors.toList()))
                            .build());
            ProductReleaseTaskStatusEnum finalStatus = ProductReleaseTaskStatusEnum.RUNNING;
            boolean hasRunning = false;
            List<String> errorMessages = new ArrayList<>();
            for (AppPackageTaskDO appPackageTask : appPackageTasks) {
                AppPackageTaskStatusEnum taskStatus = Enums.getIfPresent(AppPackageTaskStatusEnum.class,
                        appPackageTask.getTaskStatus()).orNull();
                if (taskStatus == null) {
                    finalStatus = ProductReleaseTaskStatusEnum.EXCEPTION;
                    errorMessages.add(String.format("invalid app package task found in database|taskId=%s|" +
                            "appPackageTask=%s", taskId, JSONObject.toJSONString(appPackageTask)));
                    break;
                }

                switch (taskStatus) {
                    case FAILURE:
                        finalStatus = ProductReleaseTaskStatusEnum.FAILURE;
                        errorMessages.add(String.format("failed app package task id %d", appPackageTask.getId()));
                        break;
                    case CREATED:
                    case APP_PACK_RUN:
                    case COM_PACK_RUN:
                        hasRunning = true;
                        break;
                    default:
                        break;
                }
            }
            if (!hasRunning && finalStatus.equals(ProductReleaseTaskStatusEnum.RUNNING)) {
                finalStatus = ProductReleaseTaskStatusEnum.SUCCESS;
            }
            task.setStatus(finalStatus.toString());
            task.setErrorMessage(String.join("\n", errorMessages));
            int count = productReleaseService.updateProductReleaseTask(task);
            if (count == 0) {
                log.info("failed to update product release task record, another process first, skip|taskId={}|" +
                        "status={}|errorMessage={}", taskId, finalStatus, task.getErrorMessage());
            } else {
                log.info("update product release task record success|taskId={}|status={}|errorMessage={}", taskId,
                        finalStatus, task.getErrorMessage());
            }
        }
    }
}
