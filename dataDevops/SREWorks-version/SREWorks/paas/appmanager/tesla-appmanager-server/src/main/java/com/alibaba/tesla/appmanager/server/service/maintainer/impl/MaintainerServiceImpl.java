package com.alibaba.tesla.appmanager.server.service.maintainer.impl;

import com.alibaba.tesla.appmanager.deployconfig.repository.DeployConfigRepository;
import com.alibaba.tesla.appmanager.deployconfig.repository.condition.DeployConfigQueryCondition;
import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigDO;
import com.alibaba.tesla.appmanager.meta.helm.repository.HelmMetaRepository;
import com.alibaba.tesla.appmanager.meta.helm.repository.condition.HelmMetaQueryCondition;
import com.alibaba.tesla.appmanager.meta.helm.repository.domain.HelmMetaDO;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.K8sMicroServiceMetaRepository;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.condition.K8sMicroserviceMetaQueryCondition;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain.K8sMicroServiceMetaDO;
import com.alibaba.tesla.appmanager.server.repository.AppAddonRepository;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppAddonQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppAddonDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;
import com.alibaba.tesla.appmanager.server.service.maintainer.MaintainerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 系统维护 Service
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class MaintainerServiceImpl implements MaintainerService {

    @Autowired
    private AppAddonRepository appAddonRepository;

    @Autowired
    private DeployConfigRepository deployConfigRepository;

    @Autowired
    private ComponentPackageTaskRepository componentPackageTaskRepository;

    @Autowired
    private K8sMicroServiceMetaRepository k8sMicroServiceMetaRepository;

    @Autowired
    private HelmMetaRepository helmMetaRepository;

    /**
     * 升级 namespaceId / stageId (针对各 meta 表新增的 namespaceId / stageId 空字段进行初始化)
     *
     * @param namespaceId Namespace ID
     * @param stageId     Stage ID
     */
    @Override
    public void upgradeNamespaceStage(String namespaceId, String stageId) {
        upgradeNamespaceStageForAppAddon(namespaceId, stageId);
        upgradeNamespaceStageForDeployConfig(namespaceId, stageId);
        upgradeNamespaceStageForComponentPackageTasks(namespaceId, stageId);
        upgradeNamespaceStageForK8sMicroServices(namespaceId, stageId);
        upgradeNamespaceStageForHelm(namespaceId, stageId);
    }

    /**
     * 升级 helm 中的 namespaceId / stageId
     *
     * @param namespaceId Namespace ID
     * @param stageId     Stage ID
     */
    private void upgradeNamespaceStageForHelm(String namespaceId, String stageId) {
        List<HelmMetaDO> records = helmMetaRepository.selectByCondition(HelmMetaQueryCondition.builder()
                .namespaceIdNotEqualTo(namespaceId)
                .build());
        List<HelmMetaDO> stageRecords = helmMetaRepository.selectByCondition(HelmMetaQueryCondition.builder()
                .stageIdNotEqualTo(stageId)
                .build());
        CollectionUtils.addAll(records, stageRecords);
        Set<Long> usedSet = new HashSet<>();
        for (HelmMetaDO record : records) {
            if (usedSet.contains(record.getId())) {
                continue;
            }
            usedSet.add(record.getId());
            String rawNamespaceId = record.getNamespaceId();
            String rawStageId = record.getStageId();
            record.setNamespaceId(namespaceId);
            record.setStageId(stageId);
            HelmMetaQueryCondition findCondition = HelmMetaQueryCondition.builder().id(record.getId()).build();
            try {
                helmMetaRepository.updateByCondition(record, findCondition);
                log.info("upgrade namespace and stage field in helm record|appId={}|namespaceId={}|" +
                                "stageId={}|componentType={}|name={}|helmPackageId={}", record.getAppId(),
                        rawNamespaceId, rawStageId, record.getComponentType(), record.getName(),
                        record.getHelmPackageId());
            } catch (Exception e) {
                if (e.getMessage().contains("Duplicate entry")) {
                    helmMetaRepository.deleteByCondition(findCondition);
                    log.error("find conflict helm microservice record in upgrading namespace and stage field " +
                                    "progress, delete it|appId={}|namespaceId={}|stageId={}|componentType={}|" +
                                    "helmPackageId={}", record.getAppId(), rawNamespaceId, rawStageId,
                            record.getComponentType(), record.getHelmPackageId());
                } else {
                    log.error("upgrade namespace and stage field in helm record failed|appId={}|namespaceId={}|" +
                                    "stageId={}|componentType={}|name={}|helmPackageId={}|exception={}",
                            record.getAppId(), rawNamespaceId, rawStageId, record.getComponentType(), record.getName(),
                            record.getHelmPackageId(), ExceptionUtils.getStackTrace(e));
                }
            }
        }
    }

    /**
     * 升级 k8s microservices 中的 namespaceId / stageId
     *
     * @param namespaceId Namespace ID
     * @param stageId     Stage ID
     */
    private void upgradeNamespaceStageForK8sMicroServices(String namespaceId, String stageId) {
        List<K8sMicroServiceMetaDO> records = k8sMicroServiceMetaRepository
                .selectByCondition(K8sMicroserviceMetaQueryCondition.builder()
                        .namespaceIdNotEqualTo(namespaceId)
                        .build());
        List<K8sMicroServiceMetaDO> stageRecords = k8sMicroServiceMetaRepository
                .selectByCondition(K8sMicroserviceMetaQueryCondition.builder()
                        .stageIdNotEqualTo(stageId)
                        .build());
        CollectionUtils.addAll(records, stageRecords);
        Set<Long> usedSet = new HashSet<>();
        for (K8sMicroServiceMetaDO record : records) {
            if (usedSet.contains(record.getId())) {
                continue;
            }
            usedSet.add(record.getId());
            String rawNamespaceId = record.getNamespaceId();
            String rawStageId = record.getStageId();
            record.setNamespaceId(namespaceId);
            record.setStageId(stageId);
            K8sMicroserviceMetaQueryCondition findCondition = K8sMicroserviceMetaQueryCondition.builder()
                    .id(record.getId())
                    .build();
            try {
                k8sMicroServiceMetaRepository.updateByCondition(record, findCondition);
                log.info("upgrade namespace and stage field in k8s microservice record|appId={}|namespaceId={}|" +
                                "stageId={}|componentType={}|microserviceId={}", record.getAppId(),
                        rawNamespaceId, rawStageId, record.getComponentType(), record.getMicroServiceId());
            } catch (Exception e) {
                if (e.getMessage().contains("Duplicate entry")) {
                    k8sMicroServiceMetaRepository.deleteByCondition(findCondition);
                    log.error("find conflict k8s microservice record in upgrading namespace and stage field " +
                                    "progress, delete it|appId={}|namespaceId={}|stageId={}|componentType={}|" +
                                    "microserviceId={}", record.getAppId(), rawNamespaceId, rawStageId,
                            record.getComponentType(), record.getMicroServiceId());
                } else {
                    log.error("upgrade namespace and stage field in k8s microservice record failed|appId={}|" +
                                    "namespaceId={}|stageId={}|componentType={}|microserviceId={}|exception={}",
                            record.getAppId(), rawNamespaceId, rawStageId, record.getComponentType(),
                            record.getMicroServiceId(), ExceptionUtils.getStackTrace(e));
                }
            }
        }
    }

    /**
     * 升级 component package tasks 中的 namespaceId / stageId
     *
     * @param namespaceId Namespace ID
     * @param stageId     Stage ID
     */
    private void upgradeNamespaceStageForComponentPackageTasks(String namespaceId, String stageId) {
        List<ComponentPackageTaskDO> records = componentPackageTaskRepository
                .selectByCondition(ComponentPackageTaskQueryCondition.builder()
                        .namespaceIdNotEqualTo(namespaceId)
                        .build());
        List<ComponentPackageTaskDO> stageRecords = componentPackageTaskRepository
                .selectByCondition(ComponentPackageTaskQueryCondition.builder()
                        .stageIdNotEqualTo(stageId)
                        .build());
        CollectionUtils.addAll(records, stageRecords);
        Set<Long> usedSet = new HashSet<>();
        for (ComponentPackageTaskDO record : records) {
            if (usedSet.contains(record.getId())) {
                continue;
            }
            usedSet.add(record.getId());
            String rawNamespaceId = record.getNamespaceId();
            String rawStageId = record.getStageId();
            record.setNamespaceId(namespaceId);
            record.setStageId(stageId);
            ComponentPackageTaskQueryCondition findCondition = ComponentPackageTaskQueryCondition.builder()
                    .id(record.getId())
                    .build();
            try {
                componentPackageTaskRepository.updateByCondition(record, findCondition);
                log.info("upgrade namespace and stage field in component package task record|appId={}|namespaceId={}|" +
                                "stageId={}|componentType={}|componentName={}|packageVersion={}", record.getAppId(),
                        rawNamespaceId, rawStageId, record.getComponentType(), record.getComponentName(),
                        record.getPackageVersion());
            } catch (Exception e) {
                if (e.getMessage().contains("Duplicate entry")) {
                    componentPackageTaskRepository.deleteByCondition(findCondition);
                    log.error("find conflict component package tasks record in upgrading namespace and stage field " +
                            "progress, delete it|appId={}|namespaceId={}|stageId={}|componentType={}|" +
                            "componentName={}|packageVersion={}", record.getAppId(), rawNamespaceId, rawStageId,
                            record.getComponentType(), record.getComponentName(), record.getPackageVersion());
                } else {
                    log.error("upgrade namespace and stage field in component package task record failed|appId={}|" +
                                    "namespaceId={}|stageId={}|componentType={}|componentName={}|packageVersion={}|" +
                                    "exception={}", record.getAppId(), rawNamespaceId, rawStageId,
                            record.getComponentType(), record.getComponentName(), record.getPackageVersion(),
                            ExceptionUtils.getStackTrace(e));
                }
            }
        }
    }

    /**
     * 升级 deploy config 中的 namespaceId / stageId
     *
     * @param namespaceId Namespace ID
     * @param stageId     Stage ID
     */
    private void upgradeNamespaceStageForDeployConfig(String namespaceId, String stageId) {
        List<DeployConfigDO> records = deployConfigRepository.selectByCondition(DeployConfigQueryCondition.builder()
                .isolateNamespaceIdNotEqualTo(namespaceId)
                .build());
        List<DeployConfigDO> stageRecords = deployConfigRepository.selectByCondition(DeployConfigQueryCondition.builder()
                .isolateStageIdNotEqualTo(stageId)
                .build());
        CollectionUtils.addAll(records, stageRecords);
        Set<Long> usedSet = new HashSet<>();
        for (DeployConfigDO record : records) {
            if (usedSet.contains(record.getId())) {
                continue;
            }
            usedSet.add(record.getId());
            String rawNamespaceId = record.getNamespaceId();
            String rawStageId = record.getStageId();
            record.setNamespaceId(namespaceId);
            record.setStageId(stageId);
            DeployConfigQueryCondition findCondition = DeployConfigQueryCondition.builder().id(record.getId()).build();
            try {
                deployConfigRepository.updateByCondition(record, findCondition);
                log.info("upgrade namespace and stage field in deploy config record|appId={}|typeId={}|envId={}|" +
                                "inherit={}|namespaceId={}|stageId={}", record.getAppId(), record.getTypeId(),
                        record.getEnabled(), record.getInherit(), rawNamespaceId, rawStageId);
            } catch (Exception e) {
                if (e.getMessage().contains("Duplicate entry")) {
                    deployConfigRepository.deleteByCondition(findCondition);
                    log.error("find conflict deploy config record in upgrading namespace and stage field progress, " +
                            "delete it|appId={}|typeId={}|envId={}|inherit={}|namespaceId={}|stageId={}",
                            record.getAppId(), record.getTypeId(), record.getEnabled(), record.getInherit(),
                            rawNamespaceId, rawStageId);
                } else {
                    log.error("upgrade namespace and stage field in deploy config record failed|appId={}|typeId={}|" +
                                    "envId={}|inherit={}|namespaceId={}|stageId={}|exception={}", record.getAppId(),
                            record.getTypeId(), record.getEnabled(), record.getInherit(), rawNamespaceId, rawStageId,
                            ExceptionUtils.getStackTrace(e));
                }
            }
        }
    }

    /**
     * 升级 app addon 中的 namespaceId / stageId
     *
     * @param namespaceId Namespace ID
     * @param stageId     Stage ID
     */
    private void upgradeNamespaceStageForAppAddon(String namespaceId, String stageId) {
        List<AppAddonDO> records = appAddonRepository.selectByCondition(AppAddonQueryCondition.builder()
                .namespaceIdNotEqualTo(namespaceId)
                .build());
        List<AppAddonDO> stageRecords = appAddonRepository.selectByCondition(AppAddonQueryCondition.builder()
                .stageIdNotEqualTo(stageId)
                .build());
        CollectionUtils.addAll(records, stageRecords);
        Set<Long> usedSet = new HashSet<>();
        for (AppAddonDO record : records) {
            if (usedSet.contains(record.getId())) {
                continue;
            }
            usedSet.add(record.getId());
            String rawNamespaceId = record.getNamespaceId();
            String rawStageId = record.getStageId();
            record.setNamespaceId(namespaceId);
            record.setStageId(stageId);
            AppAddonQueryCondition findCondition = AppAddonQueryCondition.builder().id(record.getId()).build();
            try {
                int count = appAddonRepository.updateByCondition(record, findCondition);
                if (count > 0) {
                    log.info("upgrade namespace and stage field in app addon record|appId={}|namespaceId={}|" +
                                    "stageId={}|appAddonId={}", record.getAppId(), rawNamespaceId, rawStageId,
                            record.getId());
                } else {
                    log.error("upgrade namespace and stage field failed, count=0|appId={}|namespaceId={}|stageId={}|" +
                                    "appAddonId={}", record.getAppId(), rawNamespaceId, rawStageId, record.getId());
                }
            } catch (Exception e) {
                if (e.getMessage().contains("Duplicate entry")) {
                    appAddonRepository.deleteByCondition(findCondition);
                    log.error("find conflict app addon record in upgrading namespace and stage field progress, " +
                                    "delete it|appId={}|namespaceId={}|stageId={}|appAddonId={}",
                            record.getAppId(), rawNamespaceId, rawStageId, record.getId());
                } else {
                    log.error("upgrade namespace and stage field in app addon record failed|appId={}|namespaceId={}|" +
                                    "stageId={}|appAddonId={}|exception={}", record.getAppId(), rawNamespaceId, rawStageId,
                            record.getId(), ExceptionUtils.getStackTrace(e));
                }
            }
        }
    }
}
