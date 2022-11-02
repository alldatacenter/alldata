package com.alibaba.tesla.appmanager.server.service.pack.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.ComponentPackageProvider;
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.AppPackageTaskStatusEnum;
import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskStateEnum;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.container.ComponentPackageTaskMessage;
import com.alibaba.tesla.appmanager.domain.dto.ComponentPackageTaskDTO;
import com.alibaba.tesla.appmanager.domain.dto.EnvMetaDTO;
import com.alibaba.tesla.appmanager.domain.req.apppackage.ComponentBinder;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageTaskCreateReq;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageTaskQueryReq;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageTaskRetryReq;
import com.alibaba.tesla.appmanager.meta.helm.repository.condition.HelmMetaQueryCondition;
import com.alibaba.tesla.appmanager.meta.helm.repository.domain.HelmMetaDO;
import com.alibaba.tesla.appmanager.meta.helm.service.HelmMetaService;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.condition.K8sMicroserviceMetaQueryCondition;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain.K8sMicroServiceMetaDO;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.service.K8sMicroserviceMetaService;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.util.K8sMicroServiceUtil;
import com.alibaba.tesla.appmanager.server.repository.AppPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppAddonQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.AppMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppAddonDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppMetaDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;
import com.alibaba.tesla.appmanager.server.service.appaddon.AppAddonService;
import com.alibaba.tesla.appmanager.server.service.appmeta.AppMetaService;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageTaskService;
import com.alibaba.tesla.appmanager.server.service.pack.PackService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.alibaba.tesla.appmanager.common.constants.DefaultConstant.INTERNAL_ADDON_APP_META;
import static com.alibaba.tesla.appmanager.common.constants.DefaultConstant.INTERNAL_ADDON_DEVELOPMENT_META;

/**
 * 打包服务
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Service
@Slf4j
public class PackServiceImpl implements PackService {

    private final ComponentPackageProvider componentPackageProvider;
    private final ComponentPackageTaskRepository componentPackageTaskRepository;
    private final K8sMicroserviceMetaService k8sMicroserviceMetaService;
    private final AppPackageTaskRepository appPackageTaskRepository;
    private final AppPackageTaskService appPackageTaskService;
    private final AppAddonService appAddonService;
    private final AppMetaService appMetaService;
    private final HelmMetaService helmMetaService;
    private final SystemProperties systemProperties;

    public PackServiceImpl(
            ComponentPackageProvider componentPackageProvider,
            ComponentPackageTaskRepository componentPackageTaskRepository,
            K8sMicroserviceMetaService k8sMicroserviceMetaService,
            AppPackageTaskRepository appPackageTaskRepository, AppPackageTaskService appPackageTaskService,
            AppAddonService appAddonService, AppMetaService appMetaService,
            HelmMetaService helmMetaService, SystemProperties systemProperties) {
        this.componentPackageProvider = componentPackageProvider;
        this.componentPackageTaskRepository = componentPackageTaskRepository;
        this.k8sMicroserviceMetaService = k8sMicroserviceMetaService;
        this.appPackageTaskRepository = appPackageTaskRepository;
        this.appPackageTaskService = appPackageTaskService;
        this.appAddonService = appAddonService;
        this.appMetaService = appMetaService;
        this.helmMetaService = helmMetaService;
        this.systemProperties = systemProperties;
    }

    @Override
    public void retryComponentPackageTask(ComponentPackageTaskMessage componentPackageTaskMessageDO) {
        log.info("action=packService|retryComponentPackageTask|enter|componentPackageTask={}",
                JSONObject.toJSONString(componentPackageTaskMessageDO));

        ComponentPackageTaskQueryReq taskQueryRequest = ComponentPackageTaskQueryReq.builder()
                .componentPackageTaskId(componentPackageTaskMessageDO.getComponentPackageTaskId())
                .build();
        String operator = componentPackageTaskMessageDO.getOperator();

        ComponentPackageTaskDTO componentPackageTaskDTO = componentPackageProvider.getTask(
                taskQueryRequest, operator);

        AppPackageTaskDO appPackageTaskDO = new AppPackageTaskDO();
        appPackageTaskDO.setId(componentPackageTaskDTO.getAppPackageTaskId());
        appPackageTaskDO.setTaskStatus(AppPackageTaskStatusEnum.COM_PACK_RUN.toString());

        appPackageTaskRepository.updateByCondition(appPackageTaskDO,
                AppPackageTaskQueryCondition.builder().id(appPackageTaskDO.getId()).build());

        ComponentPackageTaskRetryReq taskRetryRequest = ComponentPackageTaskRetryReq.builder()
                .componentPackageTaskId(componentPackageTaskMessageDO.getComponentPackageTaskId())
                .build();
        componentPackageProvider.retryTask(taskRetryRequest, componentPackageTaskMessageDO.getOperator());
    }

    @Override
    public void createComponentPackageTask(ComponentPackageTaskMessage message) {
        ComponentBinder component = message.getComponent();
        String appId = message.getAppId();
        String namespaceId = message.getNamespaceId();
        String stageId = message.getStageId();
        ComponentTypeEnum componentType = component.getComponentType();
        String componentName = component.getComponentName();
        ComponentPackageTaskCreateReq request = ComponentPackageTaskCreateReq.builder()
                .appId(appId)
                .namespaceId(namespaceId)
                .stageId(stageId)
                .appPackageTaskId(message.getAppPackageTaskId())
                .componentType(componentType.toString())
                .componentName(componentName)
                .version(component.getVersion())
                .build();
        
        try {
            request.setOptions(generateBuildOptions(message));
            log.info("action=packService|doComponentPackageCreate|request={}", JSONObject.toJSONString(request));
            componentPackageProvider.createTask(request, message.getOperator());
        } catch (Exception e) {
            log.error("action=packService|doComponentPackageCreate|message={}|exception={}", e.getMessage(),
                    ExceptionUtils.getStackTrace(e));
            ComponentPackageTaskDO taskDO = ComponentPackageTaskDO.builder()
                    .appId(appId)
                    .namespaceId(namespaceId)
                    .stageId(stageId)
                    .componentType(componentType.toString())
                    .componentName(componentName)
                    .packageVersion(component.getVersion())
                    .packageCreator(message.getOperator())
                    .taskStatus(ComponentPackageTaskStateEnum.FAILURE.toString())
                    .appPackageTaskId(message.getAppPackageTaskId())
                    .taskLog(e.getMessage())
                    .build();
            componentPackageTaskRepository.insert(taskDO);
        }
    }

    /**
     * 生成组件包构建所需的 options 对象 (build.yaml)
     *
     * @param message 构建请求信息
     * @return options (build.yaml -> json)
     */
    @Override
    public JSONObject generateBuildOptions(ComponentPackageTaskMessage message) {
        ComponentBinder component = message.getComponent();
        String appId = message.getAppId();
        String namespaceId = message.getNamespaceId();
        String stageId = message.getStageId();
        ComponentTypeEnum componentType = component.getComponentType();
        String componentName = component.getComponentName();
        Boolean isDevelop = component.getIsDevelop();

        JSONObject options = new JSONObject();
        if (BooleanUtils.isTrue(component.getUseRawOptions())) {
            options = component.getOptions();
        } else if (componentType.isKubernetesJob() || componentType.isKubernetesMicroservice()) {
            options = buildOptions4K8sMicroService(
                    appId, namespaceId, stageId, componentName, component.getBranch());
        } else if (componentType.isResourceAddon()) {
            String[] arr = componentName.split("@", 2);
            if (arr.length != 2) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("invalid resource addon name %s", componentName));
            }
            String addonId = arr[0];
            String addonName = arr[1];
            options = buildOptions4ResourceAddon(appId, namespaceId, stageId, addonId, addonName);
        } else if (componentType.isInternalAddon()) {
            options = buildOptions4InternalAddon(appId, namespaceId, stageId, componentName, isDevelop);
        } else if (componentType.isHelm()) {
            options = buildOptions4Helm(appId, componentName, component.getBranch());
        }

        // 存储组件默认部署配置 (可选)
        if (StringUtils.isNotEmpty(component.getComponentConfiguration())) {
            options.put("componentConfiguration", component.getComponentConfiguration());
        }

        // 针对前端配置的参数/Trait/依赖关系，全部存储到 Options 中
        options.put("binderParameterValues", new JSONArray());
        options.put("binderTraits", new JSONArray());
        options.put("binderDependencies", new JSONArray());
        if (CollectionUtils.isNotEmpty(component.getParamBinderList())) {
            options.put("binderParameterValues",
                    JSONArray.parseArray(JSONArray.toJSONString(component.getParamBinderList())));
        }
        if (CollectionUtils.isNotEmpty(component.getTraitBinderList())) {
            options.put("binderTraits",
                    JSONArray.parseArray(JSONArray.toJSONString(component.getTraitBinderList())));
        }
        if (CollectionUtils.isNotEmpty(component.getDependComponentList())) {
            options.put("binderDependencies", component.getDependComponentList());
        }
        return options;
    }

    @Override
    public JSONObject buildOptions4InternalAddon(
            String appId, String namespaceId, String stageId, String addonId, Boolean isDevelop) {
        log.info("action=packService|buildOptions4InternalAddon|enter|appId={}|addonId={}|namespaceId={}|" +
                        "stageId={}|isDevelop={}", appId, addonId, namespaceId, stageId, isDevelop);
        if (INTERNAL_ADDON_DEVELOPMENT_META.equals(addonId)) {
            return new JSONObject();
        }
        if (INTERNAL_ADDON_APP_META.equals(addonId)) {
            return new JSONObject();
        }
        AppAddonQueryCondition condition = AppAddonQueryCondition.builder()
                .appId(appId)
                .namespaceId(namespaceId)
                .stageId(stageId)
                .addonId(addonId)
                .addonName(addonId)
                .build();
        AppAddonDO appAddon = appAddonService.get(condition);
        if (appAddon == null) {
            log.error("action=packService|buildOptions4InternalAddon|ERROR|message=addon not exists|appId={}|" +
                    "addonId={}", appId, addonId);
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("addon not exists|appId=%s|addonId=%s", appId, addonId));
        }

        JSONObject addonConfigJson = JSON.parseObject(appAddon.getAddonConfig());
        JSONObject result = new JSONObject();
        if (addonConfigJson.containsKey("common")) {
            result = addonConfigJson.getJSONObject("common");
        }
        result.put("isDevelop", isDevelop);
        return result;
    }

    @Override
    public JSONObject buildOptions4ResourceAddon(
            String appId, String namespaceId, String stageId, String addonId, String addonName) {
        log.info("action=packService|buildOptions4ResourceAddon|enter|appId={}|namespaceId={}|stageId={}|" +
                "addonId={}|addonName={}", appId, namespaceId, stageId, addonId, addonName);
        AppAddonQueryCondition condition = AppAddonQueryCondition.builder()
                .appId(appId)
                .namespaceId(namespaceId)
                .stageId(stageId)
                .addonId(addonId)
                .addonName(addonName)
                .build();
        AppAddonDO appAddon = appAddonService.get(condition);
        if (appAddon == null) {
            log.error("action=packService|buildOptions4ResourceAddon|ERROR|message=addon not exists|appId={}|" +
                    "addonId={}|addonName={}", appId, addonId, addonName);
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("addon not exists|appId=%s|addonId=%s|addonName=%s", appId, addonId, addonName));
        }

        JSONObject addonConfigJson = JSON.parseObject(appAddon.getAddonConfig());
        if (addonConfigJson.containsKey("common")) {
            return addonConfigJson.getJSONObject("common");
        }
        return new JSONObject();
    }

    @Override
    public JSONObject buildOptions4K8sMicroService(
            String appId, String namespaceId, String stageId, String microServiceId, String branch) {
        log.info("action=packService|buildOptions4K8sMicroService|enter|appId={}|namespaceId={}|stageId={}|" +
                "microServiceId={}|branch={}", appId, namespaceId, stageId, microServiceId, branch);
        K8sMicroserviceMetaQueryCondition microserviceMetaCondition = K8sMicroserviceMetaQueryCondition.builder()
                .appId(appId)
                .namespaceId(namespaceId)
                .stageId(stageId)
                .microServiceId(microServiceId)
                .withBlobs(true)
                .build();
        Pagination<K8sMicroServiceMetaDO> microserviceMetaList = k8sMicroserviceMetaService
                .list(microserviceMetaCondition);
        if (microserviceMetaList.isEmpty()) {
            log.error("action=packService|buildOptions4K8sMicroService|ERROR|message=component not exists|appId={}|" +
                    "microServiceId={}|namespaceId={}|stageId={}", appId, microServiceId, namespaceId, stageId);
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "component not exists");
        }

        AppMetaQueryCondition appMetaCondition = AppMetaQueryCondition.builder()
                .appId(appId)
                .withBlobs(true)
                .build();
        Pagination<AppMetaDO> appMetaList = appMetaService.list(appMetaCondition);
        if (appMetaList.isEmpty()) {
            log.error("action=packService|buildOptions4K8sMicroService|ERROR|message=app not exists|appId={}", appId);
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "app not exists");
        }

        // 如果存在多条 arch 记录，那么进行多 arch options 适配
        String dockerRegistry = systemProperties.getDockerRegistry();
        String dockerNamespace = systemProperties.getDockerNamespace();
        List<K8sMicroServiceMetaDO> items = microserviceMetaList.getItems();
        if (items.size() == 0) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "empty k8s microservice list");
        }

        if (items.size() == 1) {
            return K8sMicroServiceUtil.replaceOptionsBranch(
                    items.get(0).getComponentType(), items.get(0).getOptions(), branch, dockerRegistry, dockerNamespace);
        } else {
            JSONObject options = new JSONObject();
            Set<String> envSet = new HashSet<>();
            options.put("arch", new JSONObject());
            for (K8sMicroServiceMetaDO item : items) {
                String arch = item.getArch();
                if (StringUtils.isEmpty(arch)) {
                    arch = DefaultConstant.DEFAULT_ARCH;
                }
                JSONObject current = K8sMicroServiceUtil.replaceOptionsBranch(
                        item.getComponentType(), item.getOptions(), branch, dockerRegistry, dockerNamespace);
                JSONArray currentEnv = current.getJSONArray("env");
                if (currentEnv != null && currentEnv.size() > 0) {
                    envSet.addAll(currentEnv.toJavaList(String.class));
                }
                options.getJSONObject("arch").put(arch, current);
                options.put("kind", current.getString("kind"));
            }
            options.put("env", JSONArray.parseArray(JSONArray.toJSONString(new ArrayList<>(envSet))));
            return options;
        }
    }

    @Override
    public JSONObject buildOptions4Helm(String appId, String helmPackageId, String branch) {
        log.info("action=packService|buildOptions4Helm|enter|appId={}|helmPackageId={}|branch={}",
                appId, helmPackageId, branch);

        HelmMetaQueryCondition helmMetaQueryCondition = HelmMetaQueryCondition.builder()
                .appId(appId)
                .helmPackageId(helmPackageId)
                .withBlobs(true)
                .build();
        Pagination<HelmMetaDO> helmMetaList = this.helmMetaService.list(helmMetaQueryCondition);
        if (helmMetaList.isEmpty()) {
            log.error("action=packService|buildOptions4Helm|ERROR|message=component not exists|appId={}|" +
                    "helmPackageId={}", appId, helmPackageId);
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "component not exists");
        }

        AppMetaQueryCondition appMetaCondition = AppMetaQueryCondition.builder()
                .appId(appId)
                .withBlobs(true)
                .build();
        Pagination<AppMetaDO> appMetaList = appMetaService.list(appMetaCondition);
        if (appMetaList.isEmpty()) {
            log.error("action=packService|buildOptions4Helm|ERROR|message=app not exists|appId={}", appId);
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "app not exists");
        }

        List<EnvMetaDTO> appEnvList = new ArrayList<>();
        return helmMetaList.getItems().get(0).getJSONOptions(appEnvList);
    }
}
