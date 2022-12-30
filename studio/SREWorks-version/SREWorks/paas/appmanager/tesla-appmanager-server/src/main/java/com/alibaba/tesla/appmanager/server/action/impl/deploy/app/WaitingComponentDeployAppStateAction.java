package com.alibaba.tesla.appmanager.server.action.impl.deploy.app;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.AppPackageProvider;
import com.alibaba.tesla.appmanager.common.channel.enums.DeployAppPackageConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskStateEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployAppAttrTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployAppEventEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployAppStateEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName;
import com.alibaba.tesla.appmanager.domain.dto.AppPackageDTO;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageCreateReq;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.server.action.DeployAppStateAction;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployAppEvent;
import com.alibaba.tesla.appmanager.server.event.loader.DeployAppStateActionLoadedEvent;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;
import com.alibaba.tesla.appmanager.server.service.componentpackage.ComponentPackageTaskService;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import com.google.common.base.Enums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * App 部署工单 State 处理 Action - WAITING_COMPONENT
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service("WaitingComponentDeployAppStateAction")
public class WaitingComponentDeployAppStateAction implements DeployAppStateAction, ApplicationRunner {

    public static Integer runTimeout = 3600;

    private static final int MAX_WAIT_TIMES = 7200;

    private static final int WAIT_MILLIS = 500;

    private static final DeployAppStateEnum STATE = DeployAppStateEnum.WAITING_COMPONENT;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private ComponentPackageRepository componentPackageRepository;

    @Autowired
    private AppPackageProvider appPackageProvider;

    @Autowired
    private DeployAppService deployAppService;

    @Autowired
    private ComponentPackageTaskService componentPackageTaskService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        publisher.publishEvent(new DeployAppStateActionLoadedEvent(
                this, STATE.toString(), this.getClass().getSimpleName()));
    }

    /**
     * 自身逻辑处理
     *
     * @param order   部署工单
     * @param attrMap 属性字典
     */
    @Override
    public void run(DeployAppDO order, Map<String, String> attrMap) {
        try {
            process(order, attrMap);
        } catch (AppException e) {
            if (AppErrorCode.LOCKER_VERSION_EXPIRED.equals(e.getErrorCode())) {
                log.info("locker version expired, skip|deployAppId={}", order.getId());
                return;
            }
            throw e;
        }
    }

    /**
     * 内部处理过程
     *
     * @param order   部署工单
     * @param attrMap 属性字典
     */
    private void process(DeployAppDO order, Map<String, String> attrMap) {
        String appId = order.getAppId();
        assert !StringUtils.isEmpty(appId);
        DeployAppSchema configuration = SchemaUtil.toSchema(DeployAppSchema.class,
                attrMap.get(DeployAppAttrTypeEnum.APP_CONFIGURATION.toString()));
        List<Long> packageTaskIdList = new ArrayList<>();
        for (DeployAppSchema.SpecComponent specComponent : configuration.getSpec().getComponents()) {
            DeployAppRevisionName revision = DeployAppRevisionName.valueOf(specComponent.getRevisionName());
            if (!revision.getComponentType().isNotAddon() || !revision.isEmptyVersion()) {
                continue;
            }

            Object value = specComponent.getParameterValue(DeployAppPackageConstant.KEY_COMPONENT_PACKAGE_TASK_ID);
            if (StringUtils.isEmpty(String.valueOf(value))) {
                throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                        String.format("cannot find KEY_COMPONENT_PACKAGE_TASK_ID in component parameterValues|" +
                                "specComponent=%s", JSONObject.toJSONString(specComponent)));
            }
            Long packageTaskId = Long.valueOf(String.valueOf(value));
            packageTaskIdList.add(packageTaskId);
        }
        if (packageTaskIdList.size() == 0) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "no available component package found, abort");
        }

        // 等待所有任务完成并拿到 Component Package 列表, 创建对应的 AppPackage 并设置到数据库中
        List<ComponentPackageDO> componentPackages = waitComponentPackageTaskFinished(order, packageTaskIdList);
        componentPackages.forEach(item -> {
            for (DeployAppSchema.SpecComponent specComponent : configuration.getSpec().getComponents()) {
                DeployAppRevisionName revision = DeployAppRevisionName.valueOf(specComponent.getRevisionName());
                if (!revision.getComponentType().isNotAddon() || !revision.isEmptyVersion()) {
                    continue;
                }
                if (revision.getComponentType().toString().equals(item.getComponentType())
                        && revision.getComponentName().equals(item.getComponentName())) {
                    String newRevisionName = DeployAppRevisionName.builder()
                            .componentType(revision.getComponentType())
                            .componentName(revision.getComponentName())
                            .version(item.getPackageVersion())
                            .build().revisionName();
                    specComponent.setRevisionName(newRevisionName);
                    log.info("replace revision name success|deployAppId={}|appId={}|revision={}",
                            order.getId(), appId, newRevisionName);
                    break;
                }
            }
        });


        // 创建 AppPackage
        AppPackageDTO appPackageDTO = appPackageProvider.create(AppPackageCreateReq.builder()
                .appId(appId)
                .componentPackageIdList(componentPackages.stream()
                        .map(ComponentPackageDO::getId)
                        .collect(Collectors.toList()))
                .build(), order.getDeployCreator());
        order.setAppPackageId(appPackageDTO.getId());
        order.setPackageVersion(appPackageDTO.getPackageVersion());
        deployAppService.update(order);

        // 写回 DeployAppSchema 数据到 DB 中
        configuration.getMetadata().getAnnotations().setAppPackageId(appPackageDTO.getId());
        deployAppService.updateAttr(order.getId(), DeployAppAttrTypeEnum.APP_CONFIGURATION.toString(),
                SchemaUtil.toYamlMapStr(configuration));
        publisher.publishEvent(new DeployAppEvent(this, DeployAppEventEnum.COMPONENTS_FINISHED, order.getId()));
    }

    /**
     * 等待所有的 Component Packages 任务运行完成，并获取 ComponentPackageDO 列表
     *
     * @param order             部署工单
     * @param packageTaskIdList 任务 ID 列表
     * @return ComponentPackageDO 列表
     */
    private List<ComponentPackageDO> waitComponentPackageTaskFinished(DeployAppDO order, List<Long> packageTaskIdList) {
        Long deployAppId = order.getId();
        for (int i = 0; i < MAX_WAIT_TIMES; i++) {
            ComponentPackageTaskQueryCondition condition = ComponentPackageTaskQueryCondition.builder()
                    .idList(packageTaskIdList)
                    .withBlobs(true)
                    .pagination(false)
                    .build();
            Pagination<ComponentPackageTaskDO> componentPackageTasks = componentPackageTaskService.list(condition);
            if (componentPackageTasks.isEmpty()) {
                throw new AppException(AppErrorCode.DEPLOY_ERROR,
                        String.format("cannot find specified component package tasks by id list %s|deployAppId=%d",
                                JSONArray.toJSONString(packageTaskIdList), deployAppId));
            }

            // 检测每个 component package task 是否运行完成
            long runningCount = 0L, successCount = 0L, failureCount = 0L;
            for (ComponentPackageTaskDO componentPackageTask : componentPackageTasks.getItems()) {
                ComponentPackageTaskStateEnum status = Enums.getIfPresent(ComponentPackageTaskStateEnum.class,
                        componentPackageTask.getTaskStatus()).orNull();
                assert status != null;
                switch (status) {
                    case CREATED:
                    case RUNNING:
                        runningCount++;
                        break;
                    case SUCCESS:
                    case SKIP:
                        successCount++;
                        break;
                    case FAILURE:
                        failureCount++;
                        break;
                    default:
                        log.error("unknown component package task status " + status);
                        break;
                }
            }
            if (runningCount + successCount + failureCount < packageTaskIdList.size() || runningCount > 0) {
                log.info("component package tasks running now, sleep and check again|deployAppId={}|" +
                                "packageTaskIdList={}|runningCount={}|successCount={}|failureCount={}|dbSize={}",
                        deployAppId, JSONArray.toJSONString(packageTaskIdList), runningCount, successCount, failureCount,
                        componentPackageTasks.getItems().size());
                try {
                    Thread.sleep(WAIT_MILLIS);
                } catch (InterruptedException ignored) {
                }
            } else {
                List<Long> idList = new ArrayList<>();
                for (ComponentPackageTaskDO task : componentPackageTasks.getItems()) {
                    Long componentPackageId = task.getComponentPackageId();
                    if (componentPackageId != null && componentPackageId > 0) {
                        idList.add(componentPackageId);
                    } else {
                        String errorMessage = String.format(
                                "error waiting component package tasks finished, no component package id found|" +
                                        "deployAppId=%s|appId=%s|componentType=%s|componentName=%s|packageVersion=%s|" +
                                        "envId=%s|taskStatus=%s|taskLog=%s", deployAppId, task.getAppId(),
                                task.getComponentType(), task.getComponentName(), task.getPackageVersion(),
                                task.getEnvId(), task.getTaskStatus(), task.getTaskLog());
                        log.error(errorMessage);
                        throw new AppException(AppErrorCode.DEPLOY_ERROR, errorMessage);
                    }
                }
                return componentPackageRepository.selectByCondition(ComponentPackageQueryCondition.builder()
                        .idList(idList)
                        .withBlobs(false)
                        .build());
            }
        }
        throw new AppException(AppErrorCode.DEPLOY_ERROR,
                String.format("wait for component package tasks timeout|deployAppId=%d|%s",
                        deployAppId, JSONArray.toJSONString(packageTaskIdList)));
    }
}
