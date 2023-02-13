package com.alibaba.tesla.appmanager.server.service.rtappinstance.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.constants.RedisKeyConstant;
import com.alibaba.tesla.appmanager.common.enums.AppInstanceStatusEnum;
import com.alibaba.tesla.appmanager.common.enums.ComponentInstanceStatusEnum;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.req.destroy.DestroyComponentInstanceReq;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandlerFactory;
import com.alibaba.tesla.appmanager.kubernetes.KubernetesClientFactory;
import com.alibaba.tesla.appmanager.server.dynamicscript.handler.ComponentDestroyHandler;
import com.alibaba.tesla.appmanager.server.dynamicscript.handler.ComponentHandler;
import com.alibaba.tesla.appmanager.server.repository.RtAppInstanceHistoryRepository;
import com.alibaba.tesla.appmanager.server.repository.RtAppInstanceRepository;
import com.alibaba.tesla.appmanager.server.repository.RtComponentInstanceRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.RtAppInstanceHistoryQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.RtAppInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.RtComponentInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceHistoryDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceDO;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageService;
import com.alibaba.tesla.appmanager.server.service.rtappinstance.RtAppInstanceService;
import com.google.common.base.Enums;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;

/**
 * 实时应用实例服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j(topic = "status")
public class RtAppInstanceServiceImpl implements RtAppInstanceService {

    @Autowired
    private RtAppInstanceRepository rtAppInstanceRepository;

    @Autowired
    private RtAppInstanceHistoryRepository rtAppInstanceHistoryRepository;

    @Autowired
    private RtComponentInstanceRepository rtComponentInstanceRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private AppPackageService appPackageService;

    @Autowired
    private KubernetesClientFactory kubernetesClientFactory;

    @Autowired
    private GroovyHandlerFactory groovyHandlerFactory;

    /**
     * CRD Context
     */
    private static final CustomResourceDefinitionContext CRD_CONTEXT = new CustomResourceDefinitionContext.Builder()
            .withName("applications.apps.abm.io")
            .withGroup("apps.abm.io")
            .withVersion("v1")
            .withPlural("applications")
            .withScope("Namespaced")
            .build();

    private static final String APPLICATION_CR = "{\n" +
            "    \"apiVersion\": \"apps.abm.io/v1\",\n" +
            "    \"kind\": \"Application\",\n" +
            "    \"metadata\": {\n" +
            "        \"namespace\": \"%s\",\n" +
            "        \"name\": \"%s\"\n" +
            "    },\n" +
            "    \"spec\": {}\n" +
            "}";

    /**
     * 获取当前需要更新的实例状态的 appInstanceId 集合
     *
     * @return 集合
     */
    @Override
    public Set<String> getStatusUpdateSet() {
        Set<String> set = redisTemplate.opsForSet().members(RedisKeyConstant.RT_APP_INSTANCE_STATUS_UPDATE_SET);
        if (set == null) {
            return new HashSet<>();
        }
        return set;
    }

    /**
     * 在状态更新集合中删除指定的 appInstanceId
     *
     * @param appInstanceId 应用实例 ID
     */
    @Override
    public void removeStatusUpdateSet(String appInstanceId) {
        redisTemplate.opsForSet().remove(RedisKeyConstant.RT_APP_INSTANCE_STATUS_UPDATE_SET, appInstanceId);
    }

    /**
     * 触发指定 app instance 的异步状态更新 (时间窗口内自动去重)
     *
     * @param appInstanceId 应用实例 ID
     */
    @Override
    public void asyncTriggerStatusUpdate(String appInstanceId) {
        redisTemplate.opsForSet().add(RedisKeyConstant.RT_APP_INSTANCE_STATUS_UPDATE_SET, appInstanceId);
    }

    /**
     * 触发指定 app instance 的同步状态更新
     *
     * @param appInstanceId 应用实例 ID
     */
    @Override
    public void syncTriggerStatusUpdate(String appInstanceId) {
        RtAppInstanceQueryCondition appInstanceCondition = RtAppInstanceQueryCondition.builder()
                .appInstanceId(appInstanceId)
                .build();
        RtAppInstanceDO record = rtAppInstanceRepository.getByCondition(appInstanceCondition);
        if (record == null) {
            log.warn("cannot find app instance in database|appInstanceId={}", appInstanceId);
            return;
        }
        List<RtComponentInstanceDO> componentInstances = rtComponentInstanceRepository
                .selectByCondition(RtComponentInstanceQueryCondition.builder().appInstanceId(appInstanceId).build());
        AppInstanceStatusEnum finalStatus = AppInstanceStatusEnum.PENDING;
        if (componentInstances.size() == 0) {
            LocalDateTime borderDatetime = LocalDateTime.now().minusMinutes(30);
            LocalDateTime createDatetime = LocalDateTime
                    .ofInstant(record.getGmtCreate().toInstant(), ZoneId.systemDefault());
            if (createDatetime.isBefore(borderDatetime)) {
                finalStatus = AppInstanceStatusEnum.DELETED;
            }
        }
        boolean visit = false;  // 是否可前端访问 (productops)
        boolean abort = false;  // 是否强制中止轮询
        int expiredCount = 0; // expired 计数
        for (RtComponentInstanceDO componentInstance : componentInstances) {
            if (abort) {
                break;
            }

            ComponentInstanceStatusEnum status = Enums.getIfPresent(ComponentInstanceStatusEnum.class,
                    componentInstance.getStatus()).orNull();
            if (status == null) {
                log.warn("cannot get component instance status in app instance maintain process|appInstanceId={}|" +
                                "componentInstanceId={}|componentInstance={}", appInstanceId,
                        componentInstance.getComponentInstanceId(), JSONObject.toJSONString(componentInstance));
                finalStatus = AppInstanceStatusEnum.UNKNOWN;
                abort = true;
                continue;
            }

            // 配置 visit 字段（是否可前端访问）
            if ("INTERNAL_ADDON".equals(componentInstance.getComponentType())
                    && componentInstance.getComponentName().startsWith("productops")) {
                visit = true;
            }

            switch (status) {
                case PENDING:
                    break;
                case EXPIRED:
                    expiredCount++;
                    break;
                case NOT_IMPLEMENTED:
                    finalStatus = AppInstanceStatusEnum.NOT_IMPLEMENTED;
                    abort = true;
                    break;
                case RUNNING:
                case COMPLETED:
                    if (AppInstanceStatusEnum.PENDING.equals(finalStatus)) {
                        finalStatus = AppInstanceStatusEnum.RUNNING;
                    }
                    break;
                case UPDATING:
                case PREPARING_DELETE:
                case PREPARING_UPDATE:
                    if (AppInstanceStatusEnum.PENDING.equals(finalStatus)
                            || AppInstanceStatusEnum.RUNNING.equals(finalStatus)) {
                        finalStatus = AppInstanceStatusEnum.UPDATING;
                    }
                    break;
                case WARNING:
                    if (AppInstanceStatusEnum.PENDING.equals(finalStatus)
                            || AppInstanceStatusEnum.RUNNING.equals(finalStatus)) {
                        finalStatus = AppInstanceStatusEnum.WARNING;
                    }
                    break;
                case ERROR:
                    if (AppInstanceStatusEnum.PENDING.equals(finalStatus)
                            || AppInstanceStatusEnum.RUNNING.equals(finalStatus)) {
                        finalStatus = AppInstanceStatusEnum.ERROR;
                    }
                    break;
                case FAILED:
                    finalStatus = AppInstanceStatusEnum.FAILED;
                    abort = true;
                    break;
                default:
                    finalStatus = AppInstanceStatusEnum.UNKNOWN;
                    abort = true;
                    break;
            }
        }
        if (AppInstanceStatusEnum.PENDING.equals(finalStatus) && expiredCount == componentInstances.size()) {
            finalStatus = AppInstanceStatusEnum.EXPIRED;
        }

        // 检查最新可升级包
        String latestVersion = record.getVersion();
        boolean upgrade = false;
        Pagination<AppPackageDO> appPackages = appPackageService.list(AppPackageQueryCondition.builder()
                .appId(record.getAppId())
                .tags(Collections.singletonList(DefaultConstant.ON_SALE))
                .pageSize(DefaultConstant.UNLIMITED_PAGE_SIZE)
                .build());
        if (!appPackages.isEmpty()) {
            String packageVersion = appPackages.getItems().get(0).getPackageVersion();
            if (VersionUtil.compareTo(packageVersion, record.getVersion()) > 0) {
                latestVersion = packageVersion;
                upgrade = true;
            }
        }
        record.setUpgrade(upgrade);
        record.setLatestVersion(latestVersion);
        record.setVisit(visit);
        record.setStatus(finalStatus.toString());
        int updated = rtAppInstanceRepository.updateByCondition(record, appInstanceCondition);
        if (updated == 0) {
            log.warn("update app instance failed|appInstanceId={}|visit={}|latestVersion={}|upgrade={}|finalStatus={}",
                    appInstanceId, visit, latestVersion, upgrade, finalStatus);
        } else {
            log.info("update app instance success|appInstanceId={}|visit={}|latestVersion={}|upgrade={}|finalStatus={}",
                    appInstanceId, visit, latestVersion, upgrade, finalStatus);
        }
    }

    /**
     * 获取实时 app instance 状态历史列表
     *
     * @param condition 查询条件
     * @return 分页结果
     */
    @Override
    public Pagination<RtAppInstanceHistoryDO> listHistory(RtAppInstanceHistoryQueryCondition condition) {
        List<RtAppInstanceHistoryDO> result = rtAppInstanceHistoryRepository.selectByCondition(condition);
        return Pagination.valueOf(result, Function.identity());
    }

    /**
     * 获取实时 app instance 状态列表
     *
     * @param condition 查询条件
     * @return 分页结果
     */
    @Override
    public Pagination<RtAppInstanceDO> list(RtAppInstanceQueryCondition condition) {
        List<RtAppInstanceDO> result = rtAppInstanceRepository.selectByCondition(condition);
        for (RtAppInstanceDO appInstance : result) {
            if (AppInstanceStatusEnum.PENDING.toString().equals(appInstance.getStatus())
                    || AppInstanceStatusEnum.UPDATING.toString().equals(appInstance.getStatus())) {
                asyncTriggerStatusUpdate(appInstance.getAppInstanceId());
            }
        }
        return Pagination.valueOf(result, Function.identity());
    }

    /**
     * 查询当前的应用实例，如果存在则返回；否则新增并返回
     *
     * @param condition       查询条件 (appId/clusterId/namespaceId/stageId 必选, clusterId/namespaceId/stage 可为空)
     * @param appInstanceId   如果不存在的话，新增的应用实例 ID
     * @param appInstanceName 如果不存在的话，新增的应用实例名称
     * @param version         如果不存在的话，新增的应用实例版本
     * @return 查询或新建后的实时应用实例 DO 对象
     */
    @Override
    public RtAppInstanceDO getOrCreate(
            RtAppInstanceQueryCondition condition, String appInstanceId, String appInstanceName, String version) {
        return getOrCreate(condition, appInstanceId, appInstanceName, version, 2);
    }

    /**
     * 查询当前的应用实例
     *
     * @param condition 查询条件 (appId/clusterId/namespaceId/stageId 必选, clusterId/namespaceId/stage 可为空)
     * @return 查询实时应用实例 DO 对象
     */
    @Override
    public RtAppInstanceDO get(RtAppInstanceQueryCondition condition) {
        return rtAppInstanceRepository.getByCondition(condition);
    }

    /**
     * 删除指定的应用实例
     *
     * @param appInstanceId 应用实例 ID
     * @return RtAppInstanceDO
     */
    @Override
    public int delete(String appInstanceId) {
        RtAppInstanceQueryCondition condition = RtAppInstanceQueryCondition.builder()
                .appInstanceId(appInstanceId).build();
        RtAppInstanceDO appInstance = rtAppInstanceRepository.getByCondition(condition);
        if (appInstance == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "cannot find app instance");
        }
        String appId = appInstance.getAppId();
        String clusterId = appInstance.getClusterId();
        String namespaceId = appInstance.getNamespaceId();
        String stageId = appInstance.getStageId();
        String appInstanceName = appInstance.getAppInstanceName();
        List<RtComponentInstanceDO> componentInstances = rtComponentInstanceRepository.selectByCondition(
                RtComponentInstanceQueryCondition.builder().appInstanceId(appInstanceId).build());
        log.info("prepare to delete app instance|appInstanceId={}|appId={}|clusterId={}|namespaceId={}|" +
                "stageId={}|componentInstances={}", appInstanceId, appId, clusterId, namespaceId, stageId,
                JSONObject.toJSONString(componentInstances));

        // 删除 application
        appInstance.setStatus(AppInstanceStatusEnum.DELETING.toString());
        rtAppInstanceRepository.updateByCondition(appInstance, condition);
        if (StringUtils.isNotEmpty(appInstance.getOwnerReference())) {
            DefaultKubernetesClient client = kubernetesClientFactory.get(clusterId);
            client.customResource(CRD_CONTEXT)
                    .inNamespace(namespaceId)
                    .withName(appInstanceName)
                    .delete();
        }

        // 删除 components
        for (RtComponentInstanceDO componentInstance : componentInstances) {
            String componentInstanceId = componentInstance.getComponentInstanceId();
            String componentType = componentInstance.getComponentType();
            String componentName = componentInstance.getComponentName();
            ComponentHandler componentHandler;
            try {
                if (ComponentTypeEnum.INTERNAL_ADDON.toString().equals(componentType)) {
                    componentHandler = groovyHandlerFactory.get(
                            ComponentHandler.class, DynamicScriptKindEnum.COMPONENT.toString(),
                            String.format("%s_%s", componentType, componentName));
                } else {
                    componentHandler = groovyHandlerFactory
                            .get(ComponentHandler.class, DynamicScriptKindEnum.COMPONENT.toString(), componentType);
                }
                String destroyName = componentHandler.destroyName();
                if (StringUtils.isEmpty(destroyName)) {
                    continue;
                }
                ComponentDestroyHandler componentDestroyHandler = groovyHandlerFactory.get(
                        ComponentDestroyHandler.class,
                        DynamicScriptKindEnum.COMPONENT_DESTROY.toString(), destroyName);
                componentDestroyHandler.destroy(DestroyComponentInstanceReq.builder()
                        .appId(componentInstance.getAppId())
                        .appInstanceId(appInstanceId)
                        .componentInstanceId(componentInstance.getComponentInstanceId())
                        .componentType(componentType)
                        .componentName(componentInstance.getComponentName())
                        .clusterId(componentInstance.getClusterId())
                        .namespaceId(componentInstance.getNamespaceId())
                        .stageId(componentInstance.getStageId())
                        .build());
            } catch (AppException e) {
                if (AppErrorCode.INVALID_USER_ARGS.equals(e.getErrorCode())) {
                    log.info("cannot use component type handler to delete component instance, skip|componentType={}|" +
                                    "componentName={}|componentInstanceId={}", componentType, componentName,
                            componentInstanceId);
                } else {
                    log.warn("cannot use component type handler to delete component instance, skip|componentType={}|" +
                                    "componentName={}|componentInstanceId={}|errorMessage={}", componentType,
                            componentName, componentInstanceId, e.getErrorMessage());
                }
            } catch (Exception e) {
                log.warn("cannot use component type handler to delete component instance, skip|componentType={}|" +
                        "componentInstanceId={}|exception={}", componentType, componentInstanceId,
                        ExceptionUtils.getStackTrace(e));
            } finally {
                rtComponentInstanceRepository.deleteByCondition(RtComponentInstanceQueryCondition.builder()
                        .componentInstanceId(componentInstance.getComponentInstanceId())
                        .build());
                log.info("component instance has destroyed|componentInstanceId={}|appInstanceId={}|" +
                                "componentInstance={}", componentInstanceId, appInstanceId,
                        JSONObject.toJSONString(componentInstance));
            }
        }
        return rtAppInstanceRepository.deleteByCondition(condition);
    }

    /**
     * 查询当前的应用实例，如果存在则返回；否则新增并返回
     *
     * @param condition 查询条件 (appId/clusterId/namespaceId/stageId 必选, clusterId/namespaceId/stage 可为空)
     * @return 查询或新建后的实时应用实例 DO 对象
     */
    private RtAppInstanceDO getOrCreate(
            RtAppInstanceQueryCondition condition, String appInstanceId, String appInstanceName,
            String version, int retryTimes) {
        String appId = condition.getAppId();
        String clusterId = condition.getClusterId();
        String namespaceId = condition.getNamespaceId();
        String stageId = condition.getStageId();
        if (StringUtils.isAnyEmpty(appId, version)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid parameter, appId/version is required");
        }

        // 查询到直接返回
        RtAppInstanceDO record = rtAppInstanceRepository.getByCondition(condition);
        if (record != null) {
            if (!appInstanceId.equals(record.getAppInstanceId())
                    || !version.equals(record.getVersion())
                    || (StringUtils.isNotEmpty(appInstanceName) && !appInstanceName.equals(record.getAppInstanceName()))
                    || StringUtils.isEmpty(record.getOwnerReference())) {
                String ownerReferenceStr = getOrCreateApplicationOwnerReferenceStr(
                        clusterId, namespaceId, appInstanceName);
                record.setAppInstanceId(appInstanceId);
                record.setAppInstanceName(appInstanceName);
                record.setVersion(version);
                record.setOwnerReference(ownerReferenceStr);
                int updated = rtAppInstanceRepository.updateByCondition(record, condition);
                if (updated > 0) {
                    log.info("change appInstanceId/version field in app instance record|newAppInstanceId={}|" +
                                    "newVersion={}|newAppInstanceName={}|newOwnerReference={}|condition={}",
                            appInstanceId, version, appInstanceName, ownerReferenceStr,
                            JSONObject.toJSONString(condition));
                } else {
                    log.warn("change appInstanceId/version field in app instance record failed|newAppInstanceId={}|" +
                                    "newVersion={}|newAppInstanceName={}|newOwnerReference={}|condition={}",
                            appInstanceId, version, appInstanceName, ownerReferenceStr,
                            JSONObject.toJSONString(condition));
                }
            }
            return record;
        }

        // 插入新的应用实例记录
        String ownerReferenceStr = getOrCreateApplicationOwnerReferenceStr(clusterId, namespaceId, appInstanceName);
        RtAppInstanceDO instance = RtAppInstanceDO.builder()
                .appInstanceId(appInstanceId)
                .appInstanceName(appInstanceName)
                .appId(appId)
                .clusterId(clusterId)
                .namespaceId(namespaceId)
                .stageId(stageId)
                .version(version)
                .status(AppInstanceStatusEnum.PENDING.toString())
                .latestVersion(version)
                .upgrade(false)
                .visit(false)
                .ownerReference(ownerReferenceStr)
                .parentOwnerReference("")
                .build();
        try {
            int inserted = rtAppInstanceRepository.insert(instance);
            log.info("action=rtAppInstance.create|record has inserted|instance={}|inserted={}",
                    JSONObject.toJSONString(instance), inserted);
        } catch (Exception e) {
            if (retryTimes <= 0) {
                throw e;
            } else {
                return getOrCreate(condition, version, appInstanceId, appInstanceId, retryTimes - 1);
            }
        }
        return rtAppInstanceRepository.getByCondition(condition);
    }

    /**
     * 获取或创建一个 Application CR
     *
     * @param clusterId       集群 ID
     * @param namespaceId     Namespace ID
     * @param appInstanceName 应用实例名称
     * @return
     */
    private String getOrCreateApplicationOwnerReferenceStr(
            String clusterId, String namespaceId, String appInstanceName) {
        try {
            DefaultKubernetesClient client = kubernetesClientFactory.get(clusterId);
            Map<String, Object> instance = client.customResource(CRD_CONTEXT)
                    .inNamespace(namespaceId)
                    .withName(appInstanceName)
                    .get();
            if (instance == null) {
                instance = client.customResource(CRD_CONTEXT)
                        .createOrReplace(String.format(APPLICATION_CR, namespaceId, appInstanceName));
            }
            JSONObject cr = JSONObject.parseObject(JSONObject.toJSONString(instance));
            log.info("action=rtAppInstance.getOrCreateApplicationCR|CR={}", JSONObject.toJSONString(cr));
            String apiVersion = cr.getString("apiVersion");
            String kind = cr.getString("kind");
            JSONObject metadata = cr.getJSONObject("metadata");
            String name = metadata.getString("name");
            String uid = metadata.getString("uid");
            return JSONObject.toJSONString(new OwnerReferenceBuilder()
                    .withApiVersion(apiVersion)
                    .withKind(kind)
                    .withName(name)
                    .withUid(uid)
                    .withBlockOwnerDeletion(true)
                    .withController(true)
                    .build());
        } catch (Exception e) {
            log.error("cannot getOrCreate application cr|clusterId={}|namespaceId={}|appInstanceId={}|" +
                    "exception={}", clusterId, namespaceId, appInstanceName, ExceptionUtils.getStackTrace(e));
            return "";
        }
    }
}
