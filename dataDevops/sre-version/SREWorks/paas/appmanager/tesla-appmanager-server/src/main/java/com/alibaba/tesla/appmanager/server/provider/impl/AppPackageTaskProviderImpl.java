package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.AppPackageTaskProvider;
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.AppPackageTaskStatusEnum;
import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskStateEnum;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.PackageTaskEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.PackageUtil;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.deployconfig.repository.condition.DeployConfigQueryCondition;
import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigDO;
import com.alibaba.tesla.appmanager.deployconfig.service.DeployConfigService;
import com.alibaba.tesla.appmanager.domain.container.DeployConfigTypeId;
import com.alibaba.tesla.appmanager.domain.dto.AppPackageTaskDTO;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageTaskCreateReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageTaskNextLatestVersionReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageTaskQueryReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.ComponentBinder;
import com.alibaba.tesla.appmanager.domain.res.apppackage.AppPackageTaskCreateRes;
import com.alibaba.tesla.appmanager.server.assembly.AppPackageTaskDtoConvert;
import com.alibaba.tesla.appmanager.server.event.componentpackage.ComponentPackageTaskStartEvent;
import com.alibaba.tesla.appmanager.server.repository.AppPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppOptionDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;
import com.alibaba.tesla.appmanager.server.service.appoption.AppOptionService;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 应用包任务 Provider
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Service
@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class AppPackageTaskProviderImpl implements AppPackageTaskProvider {

    @Autowired
    private AppPackageTaskService appPackageTaskService;

    @Autowired
    private AppPackageTaskDtoConvert appPackageTaskDtoConvert;

    @Autowired
    private AppPackageTaskRepository appPackageTaskRepository;

    @Autowired
    private SystemProperties systemProperties;

    @Autowired
    private AppOptionService appOptionService;

    @Autowired
    private ComponentPackageTaskRepository componentPackageTaskRepository;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private DeployConfigService deployConfigService;

    /**
     * 创建 App Package 任务
     */
    @Override
    public AppPackageTaskCreateRes create(AppPackageTaskCreateReq request, String operator) {
        String appId = request.getAppId();
        String namespaceId = request.getNamespaceId();
        String stageId = request.getStageId();
        String packageVersion = request.getVersion();
        packageVersion = checkAppPackageVersion(appId, packageVersion);

        if (CollectionUtils.isEmpty(request.getComponents())) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "missing package component");
        }

        // 获取当前应用的默认部署 YAML
        AppOptionDO swappRecord = appOptionService.getOption(appId, "swapp");
        String swapp = "";
        if (swappRecord != null) {
            swapp = swappRecord.getValue();
        }

        AppPackageTaskDO appPackageTaskDO = AppPackageTaskDO.builder()
                .appId(appId)
                .packageVersion(packageVersion)
                .packageCreator(operator)
                .taskStatus(AppPackageTaskStatusEnum.CREATED.toString())
                .packageOptions(JSON.toJSONString(request))
                .envId(systemProperties.getEnvId())
                .swapp(swapp)
                .build();
        appPackageTaskRepository.insert(appPackageTaskDO);
        Long appPackageTaskId = appPackageTaskDO.getId();

        for (ComponentBinder component : request.getComponents()) {
            ComponentTypeEnum componentType = component.getComponentType();
            String componentName = component.getComponentName();

            // 计算版本，没有提供 version 的情况下 or ADDON 组件类型则变为自动版本
            if (StringUtils.isEmpty(component.getVersion()) || componentType.isAddon()) {
                component.setVersion(DefaultConstant.AUTO_VERSION);
            }
            String fullVersion = appPackageTaskService
                    .getComponentNextVersion(appId, componentType, componentName, component.getVersion());
            component.setVersion(fullVersion);

            // 如果需要携带当前系统中的配置，额外存储到 componentBinder 中
            if (request.isStoreConfiguration()) {
                String typeId = new DeployConfigTypeId(componentType, componentName).toString();
                DeployConfigQueryCondition queryCondition = DeployConfigQueryCondition.builder()
                        .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                        .appId(appId)
                        .typeId(typeId)
                        .envId("")
                        .enabled(true)
                        .isolateNamespaceId(namespaceId)
                        .isolateStageId(stageId)
                        .build();
                DeployConfigDO deployConfig = deployConfigService.getWithInherit(queryCondition);
                if (deployConfig != null) {
                    component.setComponentConfiguration(deployConfig.getConfig());
                } else {
                    log.info("cannot find deploy config by condition, ignore componentBinder configurations|" +
                            "condition={}", JSONObject.toJSONString(queryCondition));
                }
            }

            try {
                publisher.publishEvent(new ComponentPackageTaskStartEvent(
                        this, appPackageTaskId, 0L, appId, namespaceId, stageId,
                        operator, component, PackageTaskEnum.CREATE));
            } catch (Exception e) {
                ComponentPackageTaskDO taskDO = ComponentPackageTaskDO.builder()
                        .appId(appId)
                        .namespaceId(namespaceId)
                        .stageId(stageId)
                        .componentType(componentType.toString())
                        .componentName(componentName)
                        .packageVersion(component.getVersion())
                        .packageCreator(operator)
                        .taskStatus(ComponentPackageTaskStateEnum.FAILURE.toString())
                        .appPackageTaskId(appPackageTaskId)
                        .taskLog(e.getMessage())
                        .build();
                componentPackageTaskRepository.insert(taskDO);
            }
        }
        return AppPackageTaskCreateRes.builder()
                .appPackageTaskId(appPackageTaskId)
                .packageVersion(packageVersion)
                .build();
    }

    /**
     * 查询 App Package 任务
     */
    @Override
    public Pagination<AppPackageTaskDTO> list(AppPackageTaskQueryReq request, String operator, boolean withTags) {
        AppPackageTaskQueryCondition condition = AppPackageTaskQueryCondition.builder()
                .page(request.getPage())
                .pageSize(request.getPageSize())
                .appId(request.getAppId())
                .id(request.getAppPackageTaskId())
                .withBlobs(request.isWithBlobs())
                .build();

        if (!request.isWithBlobs() && withTags) {
            condition.setWithBlobs(true);
        }
        return Pagination.transform(appPackageTaskService.list(condition), item -> appPackageTaskDtoConvert.to(item));
    }

    /**
     * 获取指定 App Package 任务
     */
    @Override
    public AppPackageTaskDTO get(AppPackageTaskQueryReq request, String operator) {
        AppPackageTaskQueryCondition condition = AppPackageTaskQueryCondition.builder()
                .id(request.getAppPackageTaskId())
                .appId(request.getAppId())
                .withBlobs(request.isWithBlobs())
                .page(DefaultConstant.DEFAULT_PAGE_NUMBER)
                .pageSize(DefaultConstant.DEFAULT_PAGE_SIZE)
                .build();
        return appPackageTaskDtoConvert.to(appPackageTaskService.get(condition));
    }

    /**
     * 获取指定 appId 对应的当前最新版本
     */
    @Override
    public String nextLatestVersion(AppPackageTaskNextLatestVersionReq request, String operator) {
        String oldVersion = "";
        Pagination<AppPackageTaskDTO> page = list(AppPackageTaskQueryReq.builder()
                .appId(request.getAppId())
                .page(1)
                .build(), operator, false);
        if (!page.isEmpty()) {
            oldVersion = page.getItems().get(0).getPackageVersion();
        }
        return VersionUtil.buildNextPatch(oldVersion);
    }

    /**
     * 检验指定的 app package 版本是否合法
     *
     * @param appId      应用 ID
     * @param newVersion AppPackage 版本
     */
    private String checkAppPackageVersion(String appId, String newVersion) {
        List<AppPackageTaskDO> tasks = appPackageTaskRepository.selectByCondition(AppPackageTaskQueryCondition.builder()
                .appId(appId)
                .pageSize(1)
                .withBlobs(false)
                .build());

        String oldVersion = DefaultConstant.INIT_VERSION;
        if (!CollectionUtils.isEmpty(tasks)) {
            oldVersion = tasks.get(0).getPackageVersion();
        }
        return PackageUtil.fullVersion(oldVersion, newVersion);
    }
}
