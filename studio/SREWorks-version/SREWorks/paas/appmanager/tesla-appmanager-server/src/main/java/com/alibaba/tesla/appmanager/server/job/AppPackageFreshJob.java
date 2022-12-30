package com.alibaba.tesla.appmanager.server.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.constants.PackAppPackageVariableKey;
import com.alibaba.tesla.appmanager.common.enums.AppPackageTaskStatusEnum;
import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskStateEnum;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.DagTypeEnum;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageTaskCreateReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.ComponentBinder;
import com.alibaba.tesla.appmanager.server.repository.AppPackageComponentRelRepository;
import com.alibaba.tesla.appmanager.server.repository.AppPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.AppPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.*;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageService;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageTagService;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageTaskService;
import com.alibaba.tesla.appmanager.server.service.componentpackage.ComponentPackageTaskService;
import com.alibaba.tesla.appmanager.server.service.pack.dag.PackAppPackageToStorageDag;
import com.alibaba.tesla.dag.services.DagInstService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author qianmo.zm@alibaba-inc.com
 * @date 2020/07/14.
 */
@Slf4j(topic = "job")
@Component
public class AppPackageFreshJob {

    @Autowired
    private AppPackageTaskRepository appPackageTaskRepository;

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private AppPackageComponentRelRepository appPackageComponentRelRepository;

    @Autowired
    private AppPackageTagService appPackageTagService;

    @Autowired
    private DagInstService dagInstService;

    @Autowired
    private AppPackageTaskService appPackageTaskService;

    @Autowired
    private ComponentPackageTaskService componentPackageTaskService;

    @Autowired
    private AppPackageService appPackageService;

    @Autowired
    private SystemProperties systemProperties;

    private static final String ERROR_PREX = "error:";

    @Scheduled(cron = "${appmanager.cron-job.app-refresh:-}")
    @SchedulerLock(name = "appPackageFreshJob")
    public void scheduledTask() {
        AppPackageTaskQueryCondition condition = AppPackageTaskQueryCondition.builder()
                .taskStatusList(Arrays.asList(
                        AppPackageTaskStatusEnum.CREATED,
                        AppPackageTaskStatusEnum.COM_PACK_RUN,
                        AppPackageTaskStatusEnum.APP_PACK_RUN))
                .envId(systemProperties.getEnvId())
                .withBlobs(true)
                .build();
        Pagination<AppPackageTaskDO> tasks = appPackageTaskService.list(condition);
        if (tasks.getTotal() > 0) {
            log.info("action=appPackageFreshJob|freshAppPackageTask|taskCount={}", tasks.getTotal());
        } else {
            log.debug("action=appPackageFreshJob|freshAppPackageTask|taskCount={}", tasks.getTotal());
        }
        if (!tasks.isEmpty()) {
            tasks.getItems().forEach(item -> {
                try {
                    freshAppPackageTask(item);
                } catch (Exception e) {
                    log.error("action=appPackageFreshJob|freshAppPackageTask|e={}", e.getMessage(), e);
                }
            });
        }
    }

    private void freshAppPackageTask(AppPackageTaskDO appPackageTaskDO) {
        ComponentPackageTaskQueryCondition condition = ComponentPackageTaskQueryCondition.builder()
                .appPackageTaskId(appPackageTaskDO.getId())
                .build();
        Pagination<ComponentPackageTaskDO> tasks = componentPackageTaskService.list(condition);
        if (tasks.isEmpty()) {
            log.debug("action=appPackageFreshJob|freshAppPackageTask|cannot find available component package tasks|" +
                    "appPackageTaskId={}", appPackageTaskDO.getId());
            return;
        }

        if (Objects.nonNull(appPackageTaskDO.getAppPackageId())) {
            AppPackageDO appPackageDO = appPackageRepository.getByCondition(
                    AppPackageQueryCondition.builder()
                            .id(appPackageTaskDO.getAppPackageId())
                            .withBlobs(true)
                            .build());

            if (StringUtils.isNotEmpty(appPackageDO.getPackagePath())) {
                if (StringUtils.startsWith(appPackageDO.getPackagePath(), ERROR_PREX)) {
                    appPackageTaskDO.setTaskStatus(AppPackageTaskStatusEnum.FAILURE.toString());
                } else {
                    appPackageTaskDO.setTaskStatus(AppPackageTaskStatusEnum.SUCCESS.toString());
                }
                appPackageTaskRepository.updateByCondition(
                        appPackageTaskDO,
                        AppPackageTaskQueryCondition.builder().id(appPackageTaskDO.getId()).build());
            }

            log.info(
                    "action=appPackageFreshJob|freshAppPackageTask|app package exists|appPackageId={}, packagePath={}",
                    appPackageDO.getId(), appPackageDO.getPackagePath());
            return;
        }

        AppPackageTaskCreateReq appPackageTaskCreateReq = JSONObject
                .parseObject(appPackageTaskDO.getPackageOptions(), AppPackageTaskCreateReq.class);

        List<ComponentBinder> components = appPackageTaskCreateReq.getComponents();
        int componentCount = CollectionUtils.size(components);
        int unStartCount = 0;
        int runningCount = 0;
        int failedCount = 0;
        int successCount = 0;

        List<String> componentPackageIdStringList = new ArrayList<>();
        for (ComponentBinder component : components) {
            ComponentPackageTaskDO componentPackageTaskDO = tasks.getItems().stream()
                    .filter(taskDO -> isSame(taskDO, component))
                    .findAny()
                    .orElse(null);

            if (Objects.isNull(componentPackageTaskDO)) {
                unStartCount++;
            } else {
                componentPackageIdStringList.add(String.valueOf(componentPackageTaskDO.getComponentPackageId()));

                String taskStatus = componentPackageTaskDO.getTaskStatus();
                if (isRunning(taskStatus)) {
                    runningCount++;
                    continue;
                }

                if (isSuccess(taskStatus)) {
                    successCount++;
                    continue;
                }

                if (isFailed(taskStatus)) {
                    failedCount++;
                    continue;
                }
            }
        }

        log.info("action=appPackageFreshJob|freshAppPackageTask|appPackageTaskId={}|componentCount={}|unStartCount={}|"
                        + "runningCount={}|failedCount={}|successCount={}", appPackageTaskDO.getId(), componentCount,
                unStartCount, runningCount, failedCount, successCount);

        AppPackageDO appPackageDO = null;
        // 全部成功
        if (Objects.equals(componentCount, successCount)) {
            appPackageDO = createAppPackage(appPackageTaskDO, tasks.getItems());
            appPackageTaskDO.setAppPackageId(appPackageDO.getId());
            appPackageTaskDO.setTaskStatus(AppPackageTaskStatusEnum.APP_PACK_RUN.toString());
            addTag(appPackageDO.getAppId(), appPackageDO.getId(), appPackageTaskCreateReq.getTags());
        } else if (runningCount > 0 || unStartCount > 0) {
            appPackageTaskDO.setTaskStatus(AppPackageTaskStatusEnum.COM_PACK_RUN.toString());
        } else if (failedCount > 0) {
            appPackageTaskDO.setTaskStatus(AppPackageTaskStatusEnum.FAILURE.toString());
        }

        appPackageTaskRepository.updateByCondition(
                appPackageTaskDO,
                AppPackageTaskQueryCondition.builder().id(appPackageTaskDO.getId()).build());

        if (Objects.nonNull(appPackageDO)) {
            // 触发生成应用包的流程
            JSONObject variables = new JSONObject();
            variables.put(DefaultConstant.DAG_TYPE, DagTypeEnum.PACK_APP_PACKAGE.toString());
            variables.put(PackAppPackageVariableKey.APP_PACKAGE_ID, appPackageTaskDO.getAppPackageId());
            variables.put(PackAppPackageVariableKey.COMPONENT_PACKAGE_ID_LIST,
                    String.join(",", componentPackageIdStringList));
            long dagInstId = 0L;
            try {
                dagInstId = dagInstService.start(PackAppPackageToStorageDag.name, variables, true);
            } catch (Exception e) {
                log.error(
                        "action=appPackageFreshJob|ERROR|start pack app package dag failed|appPackageId={}|variables={}|"
                                + "exception={}", appPackageDO.getId(), variables.toJSONString(),
                        ExceptionUtils.getStackTrace(e));
                appPackageDO.setPackagePath(ERROR_PREX + e.getMessage());
                appPackageRepository.updateByPrimaryKeySelective(appPackageDO);
            }
            log.info(
                    "action=appPackageFreshJob|start pack app package dag success|appId={}|version={}|" +
                            "componentPackageIdList={}|dagInstId={}", appPackageDO.getAppId(), appPackageDO.getPackageVersion(),
                    JSONArray.toJSONString(componentPackageIdStringList), dagInstId);
        }
    }

    private static boolean isSame(ComponentPackageTaskDO componentPackageTaskDO,
                                  ComponentBinder component) {
        return StringUtils.equals(componentPackageTaskDO.getComponentName(), component.getComponentName())
                && StringUtils.equals(componentPackageTaskDO.getComponentType(), component.getComponentType().toString());
    }

    private static boolean isRunning(String taskStatus) {
        return StringUtils.equals(ComponentPackageTaskStateEnum.CREATED.toString(), taskStatus) || StringUtils.equals(
                ComponentPackageTaskStateEnum.RUNNING.toString(), taskStatus);
    }

    private static boolean isSuccess(String taskStatus) {
        return StringUtils.equals(ComponentPackageTaskStateEnum.SUCCESS.toString(), taskStatus) || StringUtils.equals(
                ComponentPackageTaskStateEnum.SKIP.toString(), taskStatus);
    }

    private static boolean isFailed(String taskStatus) {
        return StringUtils.equals(ComponentPackageTaskStateEnum.FAILURE.toString(), taskStatus);
    }

    private void addTag(String appId, Long appPackageId, List<String> tagList) {
        if (CollectionUtils.isNotEmpty(tagList)) {
            // 去重,避免主键冲突
            tagList = tagList.stream().distinct().collect(Collectors.toList());
            tagList.forEach(tag -> {
                AppPackageTagDO tagDO = AppPackageTagDO.builder()
                        .appId(appId)
                        .appPackageId(appPackageId)
                        .tag(tag)
                        .build();
                appPackageTagService.insert(tagDO);
            });
        }
    }

    /**
     * 创建应用包
     */
    private AppPackageDO createAppPackage(
            AppPackageTaskDO appPackageTaskDO, List<ComponentPackageTaskDO> componentPackageTaskDOList) {
        String appId = appPackageTaskDO.getAppId();
        String packageVersion = appPackageTaskDO.getPackageVersion();

        AppPackageQueryCondition condition = AppPackageQueryCondition.builder()
                .appId(appId)
                .packageVersion(packageVersion)
                .build();
        Pagination<AppPackageDO> appPackages = appPackageService.list(condition);

        // 应用包已存在
        if (!appPackages.isEmpty()) {
            return appPackages.getItems().get(0);
        }

        // 创建过程
        AppPackageDO appPackageDO = AppPackageDO.builder()
                .appId(appId)
                .packageVersion(packageVersion)
                .packageCreator(appPackageTaskDO.getPackageCreator())
                .appSchema(appPackageTaskDO.getPackageOptions())
                .swapp(appPackageTaskDO.getSwapp())
                .componentCount((long) CollectionUtils.size(componentPackageTaskDOList))
                .build();
        insertAppPackage(appPackageDO, componentPackageTaskDOList);
        return appPackageDO;
    }

    /**
     * 插入指定 appPackage 数据到 DB，并插入相关 rel 引用组件 ID
     *
     * @param appPackageDO               app package 记录
     * @param componentPackageTaskDOList 引用组件
     */
    @Transactional(rollbackFor = Exception.class)
    public void insertAppPackage(AppPackageDO appPackageDO, List<ComponentPackageTaskDO> componentPackageTaskDOList) {
        appPackageRepository.insert(appPackageDO);
        for (ComponentPackageTaskDO componentPackageTaskDO : componentPackageTaskDOList) {
            AppPackageComponentRelDO rel = AppPackageComponentRelDO.builder()
                    .appId(appPackageDO.getAppId())
                    .appPackageId(appPackageDO.getId())
                    .componentPackageId(componentPackageTaskDO.getComponentPackageId())
                    .build();
            appPackageComponentRelRepository.insert(rel);
        }
    }
}
