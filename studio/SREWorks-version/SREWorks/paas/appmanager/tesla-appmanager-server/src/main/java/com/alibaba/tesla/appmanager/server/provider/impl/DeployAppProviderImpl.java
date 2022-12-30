package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.DeployAppProvider;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.*;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.dto.*;
import com.alibaba.tesla.appmanager.domain.req.apppackage.ApplicationConfigurationGenerateReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.ComponentBinder;
import com.alibaba.tesla.appmanager.domain.req.converter.AddParametersToLaunchReq;
import com.alibaba.tesla.appmanager.domain.req.deploy.*;
import com.alibaba.tesla.appmanager.domain.res.apppackage.ApplicationConfigurationGenerateRes;
import com.alibaba.tesla.appmanager.domain.res.deploy.DeployAppPackageLaunchRes;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema.*;
import com.alibaba.tesla.appmanager.domain.schema.Schema;
import com.alibaba.tesla.appmanager.server.assembly.DeployAppDtoConvert;
import com.alibaba.tesla.appmanager.server.assembly.DeployComponentDtoConvert;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployAppEvent;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployComponentEvent;
import com.alibaba.tesla.appmanager.server.repository.AppPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.*;
import com.alibaba.tesla.appmanager.server.repository.domain.*;
import com.alibaba.tesla.appmanager.server.service.addon.AddonMetaService;
import com.alibaba.tesla.appmanager.server.service.appaddon.AppAddonService;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageService;
import com.alibaba.tesla.appmanager.server.service.componentpackage.ComponentPackageService;
import com.alibaba.tesla.appmanager.server.service.converter.ConverterService;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployComponentService;
import com.alibaba.tesla.appmanager.server.service.deploy.business.DeployAppBO;
import com.alibaba.tesla.appmanager.server.service.deploy.business.DeployComponentBO;
import com.alibaba.tesla.appmanager.server.service.unit.UnitService;
import com.alibaba.tesla.appmanager.trait.repository.condition.TraitQueryCondition;
import com.alibaba.tesla.appmanager.trait.repository.domain.TraitDO;
import com.alibaba.tesla.appmanager.trait.service.TraitService;
import com.google.common.base.Enums;
import com.google.common.collect.ImmutableMap;
import com.hubspot.jinjava.Jinjava;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * App 部署单服务实现
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class DeployAppProviderImpl implements DeployAppProvider {

    @Autowired
    private DeployAppService deployAppService;

    @Autowired
    private DeployComponentService deployComponentService;

    @Autowired
    private AppPackageRepository appPackageRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Resource
    private DeployAppDtoConvert deployAppDtoConvert;

    @Resource
    private DeployComponentDtoConvert deployComponentDtoConvert;

    @Autowired
    private AddonMetaService addonMetaService;

    @Autowired
    private TraitService traitService;

    @Autowired
    private AppAddonService appAddonService;

    @Autowired
    private ComponentPackageService componentPackageService;

    @Autowired
    private AppPackageService appPackageService;

    @Autowired
    private ConverterService converterService;

    @Autowired
    private UnitService unitService;

    /**
     * 根据条件过滤查询部署单的内容
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return 分页的 DeployAppDTO 对象
     */
    @Override
    public Pagination<DeployAppDTO> list(DeployAppListReq request, String operator) {
        DeployAppQueryCondition condition = DeployAppQueryCondition.builder()
                .appId(request.getAppId())
                .clusterId(request.getClusterId())
                .namespaceId(request.getNamespaceId())
                .stageId(request.getStageId())
                .stageIdBlackList(request.getStageIdBlackList())
                .stageIdWhiteList(request.getStageIdWhiteList())
                .packageVersion(request.getPackageVersion())
                .optionKey(request.getOptionKey())
                .optionValue(request.getOptionValue())
                .page(request.getPage())
                .pageSize(request.getPageSize())
                .pagination(request.isPagination())
                .withBlobs(request.isWithBlobs())
                .build();
        if (!StringUtils.isEmpty(request.getDeployStatus())) {
            condition.setDeployStatus(Enums.getIfPresent(DeployAppStateEnum.class, request.getDeployStatus()).orNull());
        }
        Pagination<DeployAppBO> orderList = deployAppService.list(condition, false);
        return Pagination.transform(orderList, order -> deployAppDtoConvert.to(order.getOrder()));
    }

    /**
     * 查询指定部署单的状态
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return DeployAppDTO 对象
     */
    @Override
    public DeployAppDTO get(DeployAppGetReq request, String operator) {
        Long deployAppId = request.getDeployAppId();
        DeployAppBO deployAppBO = deployAppService.get(deployAppId, false);
        List<DeployComponentBO> deployComponentBOList = deployComponentService.list(
                DeployComponentQueryCondition.builder().deployAppId(deployAppId).build(), false);

        // 组装 response 数据
        DeployAppDTO response = deployAppDtoConvert.to(deployAppBO.getOrder());
        response.setDeployComponents(deployComponentBOList.stream()
                .map(p -> deployComponentDtoConvert.to(p.getSubOrder()))
                .collect(Collectors.toList()));
        return response;
    }

    /**
     * 查询指定部署单的详细属性
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return DeployAppAttrDTO 对象
     */
    @Override
    public DeployAppAttrDTO getAttr(DeployAppGetAttrReq request, String operator) {
        Long deployAppId = request.getDeployAppId();
        DeployAppBO deployAppBO = deployAppService.get(deployAppId, true);
        return DeployAppAttrDTO.builder()
                .deployAppId(deployAppId)
                .deployStatus(deployAppBO.getOrder().getDeployStatus())
                .deployErrorMessage(deployAppBO.getOrder().getDeployErrorMessage())
                .attrMap(deployAppBO.getAttrMap())
                .build();
    }

    /**
     * 查询指定 Component 部署单的详细属性
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return DeployComponentAttrDTO 对象
     */
    @Override
    public DeployComponentAttrDTO getComponentAttr(DeployAppGetComponentAttrReq request, String operator) {
        Long deployComponentId = request.getDeployComponentId();
        DeployComponentBO deployComponentBO = deployComponentService.get(deployComponentId, true);
        return DeployComponentAttrDTO.builder()
                .deployComponentId(deployComponentId)
                .deployStatus(deployComponentBO.getSubOrder().getDeployStatus())
                .deployErrorMessage(deployComponentBO.getSubOrder().getDeployErrorMessage())
                .attrMap(deployComponentBO.getAttrMap())
                .build();
    }

    /**
     * 发起一次 AppPackage 层面的部署
     *
     * @param request 请求内容。该字符串将完整保存在 DeployApp 对象的 deployExt 字段
     * @return 响应
     */
    @Override
    public DeployAppPackageLaunchRes launch(DeployAppLaunchReq request, String creator) {
        String appConfigurationSha256 = DigestUtils.sha256Hex(request.getConfiguration());
        DeployAppSchema schema = SchemaUtil.toSchema(DeployAppSchema.class, request.getConfiguration());
        renderGlobalParameterValues(schema);
        if (Boolean.parseBoolean(request.getAutoEnvironment())) {
            appendEnvironments(schema);
        }
        MetaDataAnnotations annotations = schema.getMetadata().getAnnotations();
        if (annotations == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "cannot find annotations in launch yaml");
        }
        String unitId = annotations.getUnitId();
        String clusterId = annotations.getClusterId();
        String namespaceId = annotations.getNamespaceId();
        String stageId = annotations.getStageId();

        // 根据以下三个参数获取当前发布对应的 app package version
        String appId = annotations.getAppId();
        if (request.getRemoveSuffix() != null && request.getRemoveSuffix()) {
            appId = appId.substring(0, appId.lastIndexOf("-"));
        }
        String appPackageVersion = annotations.getAppPackageVersion();
        // 优先从 URL 参数中获取 appPackageId，获取不到则从 annotations 中获取对应的参数
        Long appPackageId = annotations.getAppPackageId();
        if (request.getAppPackageId() != null && request.getAppPackageId() > 0) {
            appPackageId = request.getAppPackageId();
        }
        AppPackageDO appPackageDO = getAppPackage(appId, appPackageVersion, appPackageId);
        String packageVersion = "";
        if (appPackageDO != null) {
            appPackageId = appPackageDO.getId();
            packageVersion = appPackageDO.getPackageVersion();
        }

        // 创建 App 部署单
        Map<String, String> attrMap = new HashMap<>();
        String appConfiguration = SchemaUtil.toYamlMapStr(schema);
        attrMap.put(DeployAppAttrTypeEnum.APP_CONFIGURATION.toString(), appConfiguration);
        DeployAppDO record = DeployAppDO.builder()
                .appPackageId(appPackageId)
                .packageVersion(packageVersion)
                .appId(appId)
                .clusterId(clusterId)
                .namespaceId(namespaceId)
                .stageId(stageId)
                .configSha256(appConfigurationSha256)
                .deployStatus(DeployAppStateEnum.CREATED.toString())
                .deployCreator(creator)
                .build();
        deployAppService.create(record, attrMap);
        long deployAppId = record.getId();
        log.info("action=deployAppPackage.launch|deployAppId={}|appPackageId={}|creator={}|" +
                        "appId={}|clusterId={}|namespaceId={}|stageId={}|request={}", deployAppId, appPackageId, creator,
                appId, clusterId, namespaceId, stageId, JSONObject.toJSONString(schema));

        // 事件发送
        DeployAppEvent event = new DeployAppEvent(this, DeployAppEventEnum.START, deployAppId);
        eventPublisher.publishEvent(event);
        return DeployAppPackageLaunchRes.builder().deployAppId(deployAppId).build();
    }

    /**
     * 发起一次 AppPackage 层面的部署 (快速部署)
     *
     * @param request 请求内容。该字符串将完整保存在 DeployApp 对象的 deployExt 字段
     * @return 响应
     */
    @Override
    public DeployAppPackageLaunchRes fastLaunch(FastDeployAppLaunchReq request, String creator) {
        String swapp = request.getSwapp();
        if (StringUtils.isEmpty(swapp)) {
            ApplicationConfigurationGenerateRes ac = appPackageService.generate(
                    ApplicationConfigurationGenerateReq.builder()
                            .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                            .appId(request.getAppId())
                            .appPackageId(request.getAppPackageId())
                            .appInstanceName(request.getAppInstanceName())
                            .clusterId(request.getClusterId())
                            .namespaceId(request.getNamespaceId())
                            .stageId(request.getStageId())
                            .componentPackageConfigurationFirst(true)
                            .build());
            swapp = ac.getYaml();
        }

        DeployAppLaunchReq launchRequest = DeployAppLaunchReq.builder()
                .appPackageId(request.getAppPackageId())
                .configuration(converterService.addParametersToLaunch(AddParametersToLaunchReq.builder()
                        .parameterValues(request.getParameterValues())
                        .clusterId(request.getClusterId())
                        .namespaceId(request.getNamespaceId())
                        .stageId(request.getStageId())
                        .appId(request.getAppId())
                        .appInstanceName(request.getAppInstanceName())
                        .launchYaml(swapp)
                        .build()).getLaunchYaml())
                .build();
        return launch(launchRequest, creator);
    }

    /**
     * 重放指定部署单，生成一个新的部署单
     *
     * @param request  请求数据
     * @param operator 操作人
     * @return 部署单 ID
     */
    @Override
    public DeployAppPackageLaunchRes replay(DeployAppReplayReq request, String operator) {
        Long originDeployAppId = request.getDeployAppId();
        DeployAppBO deployAppBO = deployAppService.get(originDeployAppId, true);
        DeployAppDO order = deployAppBO.getOrder();
        if (deployAppBO.getAttrMap() == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find attr map with deploy app id %d", originDeployAppId));
        }
        String appConfiguration = deployAppBO.getAttrMap().get(DeployAppAttrTypeEnum.APP_CONFIGURATION.toString());
        if (StringUtils.isEmpty(appConfiguration)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find app configuration with deploy app id %d", originDeployAppId));
        }

        // 创建 App 部署单
        Map<String, String> attrMap = new HashMap<>();
        attrMap.put(DeployAppAttrTypeEnum.APP_CONFIGURATION.toString(), appConfiguration);
        DeployAppDO record = DeployAppDO.builder()
                .appPackageId(order.getAppPackageId())
                .packageVersion(order.getPackageVersion())
                .appId(order.getAppId())
                .clusterId(order.getClusterId())
                .namespaceId(order.getNamespaceId())
                .stageId(order.getStageId())
                .configSha256(order.getConfigSha256())
                .deployStatus(DeployAppStateEnum.CREATED.toString())
                .deployCreator(order.getDeployCreator())
                .build();
        deployAppService.create(record, attrMap);
        long deployAppId = record.getId();
        log.info("action=deployAppPackage.replay|deployAppId={}|appPackageId={}|creator={}|" +
                        "appId={}|clusterId={}|namespaceId={}|stageId={}|configSha256={}",
                deployAppId, order.getAppPackageId(), order.getDeployCreator(), order.getAppId(), order.getClusterId(),
                order.getNamespaceId(), order.getStageId(), order.getConfigSha256());

        // 事件发送
        DeployAppEvent event = new DeployAppEvent(this, DeployAppEventEnum.START, deployAppId);
        eventPublisher.publishEvent(event);
        return DeployAppPackageLaunchRes.builder().deployAppId(deployAppId).build();
    }

    /**
     * 重试指定部署单
     *
     * @param request  请求
     * @param operator 操作人
     */
    @Override
    public void retry(DeployAppRetryReq request, String operator) {
        // 应用部署单重置到 WAITING 状态，单纯等待下层状态汇聚
        Long deployAppId = request.getDeployAppId();
        eventPublisher.publishEvent(new DeployAppEvent(this, DeployAppEventEnum.OP_RETRY, deployAppId));

        // 组件部署单重置到 PROCESSING 状态进行重试
        List<DeployComponentBO> components = deployComponentService.list(DeployComponentQueryCondition.builder()
                .deployAppId(deployAppId).build(), false);
        components.stream()
                .filter(component -> DeployComponentStateEnum.WAIT_FOR_OP.toString()
                        .equals(component.getSubOrder().getDeployStatus()))
                .forEach(component -> eventPublisher.publishEvent(new DeployComponentEvent(
                        this, DeployComponentEventEnum.OP_RETRY, component.getSubOrder().getId())));
        log.info("RETRY command has sent to deployment|deployAppId={}|operator={}", deployAppId, operator);
    }

    /**
     * 终止指定部署单
     *
     * @param request  请求
     * @param operator 操作人
     */
    @Override
    public void terminate(DeployAppTerminateReq request, String operator) {
        // 应用部署单重置到 FAILURE 状态，单纯等待下层状态汇聚
        Long deployAppId = request.getDeployAppId();
        eventPublisher.publishEvent(new DeployAppEvent(this, DeployAppEventEnum.OP_TERMINATE, deployAppId));

        // 组件部署单全部发送 TERMINATE 信号
        List<DeployComponentBO> components = deployComponentService.list(DeployComponentQueryCondition.builder()
                .deployAppId(deployAppId).build(), false);
        components.forEach(component -> eventPublisher.publishEvent(new DeployComponentEvent(
                this, DeployComponentEventEnum.OP_TERMINATE, component.getSubOrder().getId())));
        log.info("TERMINATE command has sent to deployment|deployAppId={}|operator={}", deployAppId, operator);
    }

    @Override
    public String buildAppConfig(DeployAppBuildAppConfigReq request, String operator) {
        AppPackageDO appPackageDO = appPackageRepository.getByCondition(AppPackageQueryCondition.builder()
                .id(request.getAppPackageId())
                .withBlobs(false)
                .build());

        DeployAppSchema deployAppSchema = new DeployAppSchema();
        deployAppSchema.setApiVersion("core.oam.dev/v1alpha2");
        deployAppSchema.setKind("ApplicationConfiguration");

        MetaData metaData = new MetaData();
        metaData.setName("deploy-app-package");

        MetaDataAnnotations annotations = new MetaDataAnnotations();
        annotations.setAppId(appPackageDO.getAppId());
        annotations.setAppPackageId(appPackageDO.getId());
        metaData.setAnnotations(annotations);

        Spec spec = new Spec();
        for (ComponentBinder componentBinder : request.getComponentList()) {

            SpecComponent specComponent = new SpecComponent();
            specComponent.setRevisionName(getComponentRevisionName(componentBinder));
            specComponent.setScopes(getSpecComponentScope());

            Schema schema;
            try {
                schema = getSchema(appPackageDO.getAppId(), componentBinder);
            } catch (AppException e) {
                log.error("action=deployAppPackageService|generateDeployYaml|Error|e={}", e.getErrorMessage());
                return null;
            }

            specComponent.setParameterValues(getParameterValue(componentBinder, schema));

            specComponent.setTraits(getSpecComponentTrait(componentBinder));

            specComponent.setDataOutputs(getDataOutputList(componentBinder, schema));

            specComponent.setDataInputs(getDataInputList(componentBinder, schema));

            specComponent.setDependencies(getDependencyList(componentBinder, request.getComponentList()));

            spec.getComponents().add(specComponent);
        }

        deployAppSchema.setSpec(spec);

        Yaml yaml = SchemaUtil.createYaml(DeployAppSchema.class);
        return yaml.dumpAsMap(deployAppSchema);
    }

    private Schema getSchema(String appId, ComponentBinder componentBinder) {
        Yaml yaml = SchemaUtil.createYaml(ComponentSchema.class);
        if (componentBinder.getComponentType() == ComponentTypeEnum.RESOURCE_ADDON) {
            AppAddonQueryCondition condition = AppAddonQueryCondition.builder().appId(appId).addonName(
                    componentBinder.getComponentName()).build();
            Pagination<AppAddonDO> appAddonDOPage = appAddonService.list(condition);
            if (appAddonDOPage.isEmpty()) {
                throw new AppException("应用[" + appId + "]下未发现Addon[" + componentBinder.getComponentName() + "]");
            }

            AppAddonDO appAddonDO = appAddonDOPage.getItems().get(0);
            Pagination<AddonMetaDO> addonMetaDOPage = addonMetaService.list(
                    AddonMetaQueryCondition.builder()
                            .addonId(appAddonDO.getAddonId())
                            .addonVersion(appAddonDO.getAddonVersion())
                            .build());
            if (addonMetaDOPage.isEmpty()) {
                throw new AppException(
                        "未发现Addon元信息[" + appAddonDO.getAddonId() + "," + appAddonDO.getAddonVersion() + "]");
            }

            return yaml.loadAs(addonMetaDOPage.getItems().get(0).getAddonSchema(), ComponentSchema.class);
        } else {
            ComponentPackageQueryCondition condition = ComponentPackageQueryCondition.builder()
                    .appId(appId)
                    .componentName(componentBinder.getComponentName())
                    .componentType(componentBinder.getComponentType().toString())
                    .withBlobs(true)
                    .packageVersion(componentBinder.getVersion()).build();

            Pagination<ComponentPackageDO> componentPackageList = componentPackageService.list(condition);
            if (componentPackageList.isEmpty()) {
                log.error("action=deployAppPackageService|getComponentSchema|Component包不存在");
                throw new AppException(
                        "应用[" + appId + "]下未发现Component包[" + componentBinder.getComponentName() + "," + componentBinder
                                .getVersion() + "]");
            }
            return yaml.loadAs(componentPackageList.getItems().get(0).getComponentSchema(), ComponentSchema.class);
        }
    }

    private static String getComponentRevisionName(ComponentBinder componentBinder) {
        return componentBinder.getComponentType() + "|" + componentBinder.getComponentName() + "|"
                + componentBinder.getVersion();
    }

    private List<SpecComponentScope> getSpecComponentScope() {
        SpecComponentScope specComponentScope = new SpecComponentScope();
        SpecComponentScopeRef specComponentScopeRef = new SpecComponentScopeRef();
        specComponentScopeRef.setApiVersion("flyadmin.alibaba.com/v1alpha1");
        specComponentScopeRef.setKind("Namespace");
        specComponentScopeRef.setName("default");
        specComponentScope.setScopeRef(specComponentScopeRef);

        return Collections.singletonList(specComponentScope);
    }

    private List<ParameterValue> getParameterValue(ComponentBinder componentBinder, Schema schema) {
        List<ParameterValue> parameterValueList = new ArrayList<>();
        for (ParamBinderDTO paramBinderDTO : componentBinder.getParamBinderList()) {
            //未绑定插件的参数
            if (StringUtils.isNotEmpty(paramBinderDTO.getParamDefaultValue())) {
                ParameterValue parameterValue = new ParameterValue();
                parameterValue.setValue(paramBinderDTO.getParamDefaultValue());

                if (componentBinder.getComponentType() == ComponentTypeEnum.K8S_MICROSERVICE) {
                    ComponentSchema componentSchema = (ComponentSchema) schema;
                    JSONObject spec = (JSONObject) componentSchema.getSpec().getWorkload().getSpec();
                    JSONArray envArray = spec.getJSONArray("env");
                    for (int i = 0; i < envArray.size(); i++) {
                        if (StringUtils.equals(envArray.getJSONObject(i).getString("name"),
                                paramBinderDTO.getDataInputName())) {
                            parameterValue.setToFieldPaths(Collections.singletonList("spec.env[" + i + "].value"));
                            break;
                        }
                    }
                } else if (componentBinder.getComponentType() == ComponentTypeEnum.RESOURCE_ADDON) {
                    ComponentSchema addonSchema = (ComponentSchema) schema;
                    JSONObject spec = (JSONObject) addonSchema.getSpec().getWorkload().getSpec();
                    for (String key : spec.keySet()) {
                        if (StringUtils.equals(paramBinderDTO.getDataInputName(), key)) {
                            parameterValue.setToFieldPaths(Collections.singletonList("spec." + key));
                            break;
                        }
                    }
                }

                parameterValueList.add(parameterValue);
            }
        }

        return parameterValueList;
    }

    private List<SpecComponentTrait> getSpecComponentTrait(ComponentBinder componentBinder) {
        List<SpecComponentTrait> specComponentTraitList = new ArrayList<>();
        if (CollectionUtils.isEmpty(componentBinder.getTraitBinderList())) {
            return Collections.emptyList();
        }

        for (TraitBinderDTO traitBinderDTO : componentBinder.getTraitBinderList()) {
            SpecComponentTrait specComponentTrait = new SpecComponentTrait();
            specComponentTrait.setName(traitBinderDTO.getName());
            specComponentTrait.setSpec(traitBinderDTO.getSpec());

            Pagination<TraitDO> traitMetaDOPage = traitService.list(
                    TraitQueryCondition.builder()
                            .name(traitBinderDTO.getName())
                            .build(),
                    "SYSTEM");
            if (traitMetaDOPage.isEmpty()) {
                log.error("action=deployAppPackageService|getSpecComponentTrait|Trait不存在");
                return Collections.emptyList();
            }

            //TraitDO traitMetaDO = traitMetaDOPage.getItems().get(0);
            //Yaml yaml = SchemaUtil.createYaml();
            //TraitDefinition traitDefinition = yaml.loadAs(traitMetaDO.getTraitDefinition(), TraitDefinition.class);
            //traitDefinition.getSpec().getWorkload().getDataOutputs().forEach(addonDataOutput -> {
            //    DataOutput dataOutput = new DataOutput();
            //    dataOutput.setName(
            //        getTraitOutputName(componentBinder.getComponentName(), traitBinderDTO.getAddonId(),
            //            addonDataOutput.getName()));
            //    dataOutput.setFieldPath(addonDataOutput.getFieldPath());
            //    specComponentTrait.getDataOutputs().add(dataOutput);
            //});

            specComponentTraitList.add(specComponentTrait);
        }

        return specComponentTraitList;
    }

    private List<DataOutput> getDataOutputList(ComponentBinder componentBinder, Schema schema) {
        if (componentBinder.getComponentType() == ComponentTypeEnum.RESOURCE_ADDON) {
            ComponentSchema addonSchema = (ComponentSchema) schema;
            return addonSchema.getSpec().getWorkload().getDataOutputs().stream().map(addonDataOutput -> {
                DataOutput dataOutput = new DataOutput();
                dataOutput.setName(getAddonOutputName(componentBinder.getComponentName(), addonDataOutput.getName()));
                dataOutput.setFieldPath(addonDataOutput.getFieldPath());

                return dataOutput;
            }).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private List<Dependency> getDependencyList(ComponentBinder componentBinder, List<ComponentBinder> componentList) {
        if (CollectionUtils.isEmpty(componentBinder.getDependComponentList())) {
            return Collections.emptyList();
        }

        List<Dependency> dependencyList = new ArrayList<>();
        for (String dependComponentName : componentBinder.getDependComponentList()) {
            ComponentBinder dependComponent = componentList.stream().filter(
                            cb -> StringUtils.equals(dependComponentName, cb.getComponentName()))
                    .findFirst().orElse(null);

            if (Objects.nonNull(dependComponent)) {
                Dependency dependency = new Dependency();
                dependency.setComponent(dependComponent.getComponentType().toString() + "|" + dependComponentName);
                dependencyList.add(dependency);
            }
        }

        return dependencyList;
    }

    private List<DataInput> getDataInputList(ComponentBinder componentBinder, Schema schema) {
        List<DataInput> dataInputList = new ArrayList<>();
        if (componentBinder.getComponentType() == ComponentTypeEnum.K8S_MICROSERVICE) {
            ComponentSchema componentSchema = (ComponentSchema) schema;
            for (ParamBinderDTO paramBinderDTO : componentBinder.getParamBinderList()) {
                if (StringUtils.startsWith(paramBinderDTO.getDataInputName(), "spec.")) {
                    DataInput dataInput = new DataInput();

                    DataInputValueFrom dataInputValueFrom = new DataInputValueFrom();
                    dataInputValueFrom.setDataOutputName(
                            getTraitOutputName(componentBinder.getComponentName(), paramBinderDTO.getComponentName(),
                                    paramBinderDTO.getDataOutputName()));

                    dataInput.setValueFrom(dataInputValueFrom);
                    dataInput.setToFieldPaths(Collections.singletonList(paramBinderDTO.getDataInputName()));

                    dataInputList.add(dataInput);
                } else if (paramBinderDTO.getComponentType() == ComponentTypeEnum.RESOURCE_ADDON) {
                    DataInput dataInput = new DataInput();

                    DataInputValueFrom dataInputValueFrom = new DataInputValueFrom();
                    dataInputValueFrom.setDataOutputName(
                            getAddonOutputName(paramBinderDTO.getComponentName(), paramBinderDTO.getDataOutputName()));

                    dataInput.setValueFrom(dataInputValueFrom);

                    JSONObject spec = (JSONObject) componentSchema.getSpec().getWorkload().getSpec();
                    JSONArray envArray = spec.getJSONArray("env");
                    for (int i = 0; i < envArray.size(); i++) {
                        if (StringUtils.equals(envArray.getJSONObject(i).getString("name"),
                                paramBinderDTO.getDataInputName())) {
                            dataInput.setToFieldPaths(Collections.singletonList("spec.env[" + i + "].value"));
                            break;
                        }
                    }

                    dataInputList.add(dataInput);
                }
            }
        }

        return dataInputList;
    }

    private static String getTraitOutputName(String componentName, String addonId, String outputName) {
        return componentName + ".traits." + StringUtils.substringBetween(addonId, "@", ".") + ".dataOutputs."
                + outputName;
    }

    private static String getAddonOutputName(String componentName, String outputName) {
        return componentName + ".dataOutputs." + outputName;
    }

    /**
     * 对指定的部署单自动追加环境变量
     *
     * @param schema 部署单
     */
    private void appendEnvironments(DeployAppSchema schema) {
        List<ParameterValue> parameterValues = schema.getSpec().getParameterValues();
        Set<String> existKeySet = parameterValues.stream().map(ParameterValue::getName).collect(Collectors.toSet());
        Map<String, String> envMap = System.getenv();
        for (String envKey : envMap.keySet()) {
            String envValue = envMap.get(envKey);
            // 如果已经显示设置，则直接跳过
            if (existKeySet.contains(envKey)) {
                continue;
            }
            // 如果没有值，也跳过
            if (StringUtils.isEmpty(envValue)) {
                continue;
            }
            parameterValues.add(ParameterValue.builder()
                    .name(envKey)
                    .value(envValue)
                    .toFieldPaths(new ArrayList<>())
                    .build());
        }
    }

    /**
     * 渲染 global parameter values，使用环境变量进行渲染
     *
     * @param schema DeployAppSchema
     */
    private void renderGlobalParameterValues(DeployAppSchema schema) {
        Jinjava jinjava = new Jinjava();
        ImmutableMap<String, Map<String, String>> variableMap = ImmutableMap.of("env", System.getenv());
        List<ParameterValue> parameterValues = schema.getSpec().getParameterValues();
        for (ParameterValue parameterValue : parameterValues) {
            if (parameterValue.getValue() instanceof String) {
                String rawValue = String.valueOf(parameterValue.getValue());
                String value = jinjava.render(rawValue, variableMap);
                parameterValue.setValue(value);
                if (!value.equals(String.valueOf(parameterValue.getValue()))) {
                    log.info("replace global parameter value|name={}|raw={}|current={}",
                            parameterValue.getName(), rawValue, value);
                }
            }
        }
    }

    /**
     * 根据 appId / appPackageVersion / appPackageId 获取对应的 app package 信息
     * <p>
     * appPackageVersion / appPackageId 二选一
     *
     * @param appId             应用 ID
     * @param appPackageVersion 应用包版本号
     * @param appPackageId      应用包 ID
     * @return
     */
    private AppPackageDO getAppPackage(String appId, String appPackageVersion, Long appPackageId) {
        if (StringUtils.isEmpty(appId)) {
            return null;
        }

        if (appPackageId > 0) {
            return appPackageRepository.getByCondition(AppPackageQueryCondition.builder()
                    .id(appPackageId)
                    .withBlobs(false)
                    .build());
        } else if (StringUtils.isNotEmpty(appPackageVersion)) {
            return appPackageRepository.getByCondition(AppPackageQueryCondition.builder()
                    .appId(appId)
                    .packageVersion(appPackageVersion)
                    .withBlobs(false)
                    .build());
        }
        return null;
    }
}
