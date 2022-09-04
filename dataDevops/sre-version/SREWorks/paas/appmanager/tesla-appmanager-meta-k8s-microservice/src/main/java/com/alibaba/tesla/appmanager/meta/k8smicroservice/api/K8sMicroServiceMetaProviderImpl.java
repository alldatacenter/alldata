package com.alibaba.tesla.appmanager.meta.k8smicroservice.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.K8sMicroServiceMetaProvider;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.service.GitService;
import com.alibaba.tesla.appmanager.common.util.*;
import com.alibaba.tesla.appmanager.deployconfig.repository.condition.DeployConfigQueryCondition;
import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigDO;
import com.alibaba.tesla.appmanager.deployconfig.service.DeployConfigService;
import com.alibaba.tesla.appmanager.domain.container.DeployConfigTypeId;
import com.alibaba.tesla.appmanager.domain.dto.*;
import com.alibaba.tesla.appmanager.domain.req.K8sMicroServiceMetaQueryReq;
import com.alibaba.tesla.appmanager.domain.req.K8sMicroServiceMetaQuickUpdateReq;
import com.alibaba.tesla.appmanager.domain.req.K8sMicroServiceMetaUpdateByOptionReq;
import com.alibaba.tesla.appmanager.domain.req.K8sMicroServiceMetaUpdateReq;
import com.alibaba.tesla.appmanager.domain.req.deployconfig.DeployConfigDeleteReq;
import com.alibaba.tesla.appmanager.domain.req.deployconfig.DeployConfigUpdateReq;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.assembly.K8sMicroServiceMetaDtoConvert;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.condition.K8sMicroserviceMetaQueryCondition;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain.K8sMicroServiceMetaDO;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.service.K8sMicroserviceMetaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * K8S 微应用元信息接口
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Slf4j
@Service
public class K8sMicroServiceMetaProviderImpl implements K8sMicroServiceMetaProvider {

    private final K8sMicroServiceMetaDtoConvert k8sMicroServiceMetaDtoConvert;
    private final K8sMicroserviceMetaService k8SMicroserviceMetaService;
    private final GitService gitService;
    private final DeployConfigService deployConfigService;

    public K8sMicroServiceMetaProviderImpl(
            K8sMicroServiceMetaDtoConvert k8sMicroServiceMetaDtoConvert,
            K8sMicroserviceMetaService k8SMicroserviceMetaService,
            GitService gitService, DeployConfigService deployConfigService) {
        this.k8sMicroServiceMetaDtoConvert = k8sMicroServiceMetaDtoConvert;
        this.k8SMicroserviceMetaService = k8SMicroserviceMetaService;
        this.gitService = gitService;
        this.deployConfigService = deployConfigService;
    }

    /**
     * 分页查询微应用元信息
     */
    @Override
    public Pagination<K8sMicroServiceMetaDTO> list(K8sMicroServiceMetaQueryReq request) {
        K8sMicroserviceMetaQueryCondition condition = new K8sMicroserviceMetaQueryCondition();
        ClassUtil.copy(request, condition);
        Pagination<K8sMicroServiceMetaDO> metaList = k8SMicroserviceMetaService.list(condition);
        return Pagination.transform(metaList, k8sMicroServiceMetaDtoConvert::to);
    }

    /**
     * 通过微应用 ID 查询微应用元信息
     */
    @Override
    public K8sMicroServiceMetaDTO get(Long id) {
        K8sMicroserviceMetaQueryCondition condition = K8sMicroserviceMetaQueryCondition.builder()
                .id(id)
                .withBlobs(true)
                .build();
        Pagination<K8sMicroServiceMetaDO> page = k8SMicroserviceMetaService.list(condition);
        if (page.isEmpty()) {
            return null;
        }

        K8sMicroServiceMetaDO meta = page.getItems().get(0);
        return k8sMicroServiceMetaDtoConvert.to(meta);
    }

    /**
     * 通过条件查询微应用元信息
     */
    @Override
    public K8sMicroServiceMetaDTO get(K8sMicroServiceMetaQueryReq request) {
        K8sMicroserviceMetaQueryCondition condition = new K8sMicroserviceMetaQueryCondition();
        ClassUtil.copy(request, condition);
        return k8sMicroServiceMetaDtoConvert.to(k8SMicroserviceMetaService.get(condition));
    }

    /**
     * 通过微应用 ID 删除微应用元信息
     */
    @Override
    public boolean delete(Long id) {
        if (Objects.isNull(id)) {
            return true;
        }
        K8sMicroServiceMetaDTO metaDO = this.get(id);
        String typeId = new DeployConfigTypeId(ComponentTypeEnum.K8S_MICROSERVICE, metaDO.getMicroServiceId()).toString();
        deployConfigService.delete(DeployConfigDeleteReq.builder()
                .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                .appId(metaDO.getAppId())
                .typeId(typeId)
                .envId("")
                .isolateNamespaceId(metaDO.getNamespaceId())
                .isolateStageId(metaDO.getStageId())
                .build());
        k8SMicroserviceMetaService.delete(id);
        return true;
    }

    /**
     * 创建 K8S Microservice (普通)
     */
    @Override
    public K8sMicroServiceMetaDTO create(K8sMicroServiceMetaUpdateReq request) {
        K8sMicroServiceMetaDTO dto = new K8sMicroServiceMetaDTO();
        ClassUtil.copy(request, dto);
        return create(dto);
    }

    /**
     * 创建 K8S Microservice (快速)
     */
    @Override
    public K8sMicroServiceMetaDTO create(K8sMicroServiceMetaQuickUpdateReq request) {
        K8sMicroServiceMetaDTO dto = new K8sMicroServiceMetaDTO();
        ClassUtil.copy(request, dto);
        return create(dto);
    }

    /**
     * 根据 options Yaml (build.yaml) 创建 K8s MicroService
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<K8sMicroServiceMetaDTO> updateByOption(K8sMicroServiceMetaUpdateByOptionReq request) {
        String appId = request.getAppId();
        String namespaceId = request.getNamespaceId();
        String stageId = request.getStageId();
        String productId = request.getProductId();
        String releaseId = request.getReleaseId();
        JSONObject body = request.getBody();
        if (StringUtils.isAnyEmpty(appId, namespaceId, stageId) || body == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "appId/namespaceId/stageId/body are required");
        }

        List<K8sMicroServiceMetaDTO> dtoList = k8sMicroServiceMetaDtoConvert
                .to(body, appId, namespaceId, stageId, productId, releaseId);
        if (CollectionUtils.isEmpty(dtoList)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "convert options failed");
        }
        List<K8sMicroServiceMetaDTO> results = new ArrayList<>();
        for (K8sMicroServiceMetaDTO dto : dtoList) {
            String microServiceId = dto.getMicroServiceId();
            String arch = dto.getArch();
            K8sMicroServiceMetaDO current = k8SMicroserviceMetaService
                    .getByMicroServiceId(appId, microServiceId, arch, namespaceId, stageId);
            if (current == null) {
                results.add(create(dto));
                log.info("k8s microservice has created by options|appId={}|namespaceId={}|stageId={}|body={}",
                        appId, namespaceId, stageId, body.toJSONString());
            } else {
                results.add(update(dto));
                log.info("k8s microservice has updated by options|appId={}|namespaceId={}|stageId={}|body={}",
                        appId, namespaceId, stageId, body.toJSONString());
            }
        }
        return results;
    }

    /**
     * 更新 K8s Microservice (普通)
     */
    @Override
    public K8sMicroServiceMetaDTO update(K8sMicroServiceMetaUpdateReq request) {
        K8sMicroserviceMetaQueryCondition condition = K8sMicroserviceMetaQueryCondition.builder()
                .appId(request.getAppId())
                .microServiceId(request.getMicroServiceId())
                .arch(request.getArch())
                .namespaceId(request.getNamespaceId())
                .stageId(request.getStageId())
                .withBlobs(true)
                .build();
        K8sMicroServiceMetaDO dbMeta = k8SMicroserviceMetaService.get(condition);
        if (dbMeta == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find k8s microservice meta|condition=%s",
                            JSONObject.toJSONString(condition)));
        }

        // 更新记录，并在请求的基础上覆盖 imagePushObject 对象 (如果请求未提供, 兼容历史版本前端)
        K8sMicroServiceMetaDTO dto = new K8sMicroServiceMetaDTO();
        ClassUtil.copy(request, dto);
        if (dto.getImagePushObject() == null) {
            log.warn("invalid image push object from request|request={}|dto={}",
                    JSONObject.toJSONString(request), JSONObject.toJSONString(dto));
            dto.setImagePushObject(k8sMicroServiceMetaDtoConvert.to(dbMeta).getImagePushObject());
        }
        return update(dto);
    }

    /**
     * 更新 K8s Microservice (快速)
     */
    @Override
    public K8sMicroServiceMetaDTO update(K8sMicroServiceMetaQuickUpdateReq request) {
        K8sMicroServiceMetaDTO dto = new K8sMicroServiceMetaDTO();
        ClassUtil.copy(request, dto);
        return update(dto);
    }

    /**
     * 创建 K8S Microservice
     *
     * @param dto K8S Microservice DTO 对象
     * @return K8s Microservice DTO 对象
     */
    @Override
    public K8sMicroServiceMetaDTO create(K8sMicroServiceMetaDTO dto) {
        K8sMicroServiceMetaDO meta = k8sMicroServiceMetaDtoConvert.from(dto);

        // SREWorks: repo object 创建 Gitlab repo
        if (dto.getRepoObject() != null) {
            RepoDTO repo = dto.getRepoObject();
            if (!ShellUtil.check(repo.getRepo(), repo.getCiAccount(), repo.getCiToken())) {
                if (StringUtils.isNotEmpty(dto.getRepoObject().getRepoTemplateUrl())) {
                    autoCreateRepo(dto);
                } else {
                    // 如果没有设置模板且 Project 不存在，则直接创建 Project
                    GitlabUtil.createProject(repo.getRepoDomain(), repo.getRepoGroup(),
                            repo.getRepoProject(), repo.getCiToken());
                }
            }
        }

        // 非 SREworks: 检查并创建相关 repo
        if (CollectionUtils.isNotEmpty(dto.getContainerObjectList())) {
            gitService.createRepoList(dto.getContainerObjectList());
        }

        k8SMicroserviceMetaService.create(meta);
        if (EnvUtil.isSreworks()) {
            refreshDeployConfigForSreworks(dto);
        } else {
            refreshDeployConfig(dto);
        }
        K8sMicroServiceMetaDO result = k8SMicroserviceMetaService.get(K8sMicroserviceMetaQueryCondition.builder()
                .appId(dto.getAppId())
                .microServiceId(dto.getMicroServiceId())
                .namespaceId(dto.getNamespaceId())
                .stageId(dto.getStageId())
                .arch(dto.getArch())
                .withBlobs(true)
                .build());
        return k8sMicroServiceMetaDtoConvert.to(result);
    }

    /**
     * 更新 K8S Microservice
     *
     * @param dto K8S Microservice DTO 对象
     * @return K8s Microservice DTO 对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public K8sMicroServiceMetaDTO update(K8sMicroServiceMetaDTO dto) {
        K8sMicroServiceMetaDO meta = k8sMicroServiceMetaDtoConvert.from(dto);
        K8sMicroserviceMetaQueryCondition condition = K8sMicroserviceMetaQueryCondition.builder()
                .appId(dto.getAppId())
                .namespaceId(dto.getNamespaceId())
                .stageId(dto.getStageId())
                .microServiceId(dto.getMicroServiceId())
                .arch(dto.getArch())
                .withBlobs(true)
                .build();
        k8SMicroserviceMetaService.update(meta, condition);
        if (EnvUtil.isSreworks()) {
            refreshDeployConfigForSreworks(dto);
        } else {
            refreshDeployConfig(dto);
        }
        return k8sMicroServiceMetaDtoConvert.to(k8SMicroserviceMetaService.get(condition));
    }

    /**
     * 自动维护 K8S Microservice 对应的 Deploy Config 配置
     *
     * @param dto K8S Microservice DTO
     */
    private void refreshDeployConfig(K8sMicroServiceMetaDTO dto) {
        if (StringUtils.isNotEmpty(dto.getProductId()) && StringUtils.isNotEmpty(dto.getReleaseId())) {
            deployConfigService.update(DeployConfigUpdateReq.builder()
                    .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                    .appId(dto.getAppId())
                    .typeId(new DeployConfigTypeId(DeployConfigTypeId.TYPE_PARAMETER_VALUES).toString())
                    .envId("")
                    .inherit(false)
                    .config("")
                    .isolateNamespaceId(dto.getNamespaceId())
                    .isolateStageId(dto.getStageId())
                    .productId(dto.getProductId())
                    .releaseId(dto.getReleaseId())
                    .build());
            deployConfigService.update(DeployConfigUpdateReq.builder()
                    .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                    .appId(dto.getAppId())
                    .typeId(new DeployConfigTypeId(dto.getComponentType(), dto.getMicroServiceId()).toString())
                    .envId("")
                    .inherit(false)
                    .config("")
                    .isolateNamespaceId(dto.getNamespaceId())
                    .isolateStageId(dto.getStageId())
                    .productId(dto.getProductId())
                    .releaseId(dto.getReleaseId())
                    .build());
            deployConfigService.update(DeployConfigUpdateReq.builder()
                    .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                    .appId(dto.getAppId())
                    .typeId(new DeployConfigTypeId(DeployConfigTypeId.TYPE_POLICIES).toString())
                    .envId("")
                    .inherit(false)
                    .config("")
                    .isolateNamespaceId(dto.getNamespaceId())
                    .isolateStageId(dto.getStageId())
                    .productId(dto.getProductId())
                    .releaseId(dto.getReleaseId())
                    .build());
            deployConfigService.update(DeployConfigUpdateReq.builder()
                    .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                    .appId(dto.getAppId())
                    .typeId(new DeployConfigTypeId(DeployConfigTypeId.TYPE_WORKFLOW).toString())
                    .envId("")
                    .inherit(false)
                    .config("")
                    .isolateNamespaceId(dto.getNamespaceId())
                    .isolateStageId(dto.getStageId())
                    .productId(dto.getProductId())
                    .releaseId(dto.getReleaseId())
                    .build());
        }
    }

    /**
     * 根据 K8S Microservice DTO 对象自动创建 Repo
     *
     * @param dto K8S Microservice DTO 对象
     */
    private void autoCreateRepo(K8sMicroServiceMetaDTO dto) {
        RepoDTO repoDTO = dto.getRepoObject();
        GitlabUtil.createProject(repoDTO.getRepoDomain(), repoDTO.getRepoGroup(), repoDTO.getRepoProject(),
                repoDTO.getCiToken());
        String zipFile = dto.getAppId() + "." + dto.getMicroServiceId() + ".zip";
        File zipFilePath = HttpUtil.download(repoDTO.getRepoTemplateUrl(), zipFile);

        String unZipDir = StringUtils.substringBeforeLast(zipFilePath.getAbsolutePath(), ".");
        log.info(">>>k8sMicroServiceMetaProvider|autoCreateRepo|unZipDir={}", unZipDir);
        ZipUtil.unzip(zipFilePath.getAbsolutePath(), unZipDir);

        List<InitContainerDTO> initContainerList = dto.getInitContainerList();
        String scriptPath = unZipDir + File.separator + "sw.sh";
        if (CollectionUtils.isNotEmpty(initContainerList)) {
            for (InitContainerDTO initContainerDTO : initContainerList) {
                ShellUtil.init(scriptPath, initContainerDTO.createContainerName(), "initContainer",
                        initContainerDTO.createDockerFileTemplate(),
                        initContainerDTO.getType().toLowerCase());
            }
        }

        ShellUtil.push(scriptPath, repoDTO.getRepo(), repoDTO.getCiAccount(), repoDTO.getCiToken());
        FileUtil.deleteDir(zipFilePath);
        FileUtil.deleteDir(new File(unZipDir));
    }

    /**
     * 根据 K8S Microservice DTO 对象刷新当前的 Deploy Config 表中
     *
     * @param meta K8S Microservice DTO
     */
    private void refreshDeployConfigForSreworks(K8sMicroServiceMetaDTO meta) {
        LaunchDTO launchObject = meta.getLaunchObject();
        if (launchObject == null) {
            log.info("appId: " + meta.getAppId() + " launchObject is null, skip");
            return;
        }

        JSONObject configObject = new JSONObject();
        JSONArray parameterValues = new JSONArray();
        JSONArray traits = new JSONArray();
        JSONArray scopes = new JSONArray();

        JSONObject nsScopeObject = new JSONObject();
        JSONObject nsObject = new JSONObject();
        JSONObject nsSpecObject = new JSONObject();
        nsSpecObject.put("autoCreate", true);
        if (launchObject.getNamespaceResourceLimit() != null) {
            JSONObject resourceQuotaObject = new JSONObject();
            resourceQuotaObject.put("name", "sreworks-resource-limit");
            resourceQuotaObject.put("spec", launchObject.getNamespaceResourceLimit());
            nsSpecObject.put("resourceQuota", resourceQuotaObject);
        }
        nsObject.put("spec", nsSpecObject);
        nsObject.put("apiVersion", "core.oam.dev/v1alpha2");
        nsObject.put("kind", "Namespace");
        nsScopeObject.put("scopeRef", nsObject);
        scopes.add(nsScopeObject);

        configObject.put("revisionName", "K8S_MICROSERVICE|" + meta.getMicroServiceId() + "|_");

        /** - name: service.trait.abm.io
         *    runtime: post
         *    spec:
         *      ports:
         *      - protocol: TCP
         *        port: 80
         *        targetPort: 5000
         */
        JSONObject svcSpec = new JSONObject();
        JSONArray svcSpecPorts = new JSONArray();
        JSONObject portObject = new JSONObject();

        List<String> ports = new ArrayList<>(8);
        if (StringUtils.isNotBlank(launchObject.getServicePorts())) {
            ports = Arrays.stream(launchObject.getServicePorts().split(",")).collect(Collectors.toList());
        } else if (!Objects.isNull(launchObject.getServicePort())) {
            ports.add(launchObject.getServicePort().toString());
        } else {
            ports.add("7001");
        }
        for (String port : ports) {
            portObject.put("protocol", "TCP");
            if (port.split(":").length > 1) {
                portObject.put("port", port.split(":")[0]);
                portObject.put("targetPort", Long.valueOf(port.split(":")[1]));
            } else {
                portObject.put("port", 80);
                portObject.put("targetPort", Long.valueOf(port));
            }
            svcSpecPorts.add(portObject);
        }

        if (launchObject.getServiceLabels() != null) {
            svcSpec.put("labels", launchObject.getServiceLabels());
        }
        svcSpec.put("ports", svcSpecPorts);

        JSONObject svcTrait = new JSONObject();
        svcTrait.put("name", "service.trait.abm.io");
        svcTrait.put("runtime", "post");
        svcTrait.put("spec", svcSpec);
        traits.add(svcTrait);

        if (StringUtils.isNotBlank(launchObject.getGatewayRoute())) {

            /** - name: gateway.trait.abm.io
             *    runtime: post
             *    spec:
             *      path: /aiops/aisp/**
             *      servicePort: 80
             *      serviceName: '{{ Global.STAGE_ID }}-aiops-aisp.sreworks-aiops'
             */

            JSONObject gatewayTrait = new JSONObject();
            JSONObject gatewaySpec = new JSONObject();
            String gatewayRoute = launchObject.getGatewayRoute();
            if (!launchObject.getGatewayRoute().startsWith("/")) {
                gatewayRoute = "/" + gatewayRoute;
            }
            if (!launchObject.getGatewayRoute().endsWith("*")) {
                gatewayRoute = gatewayRoute + "/**";
            }
            gatewaySpec.put("path", gatewayRoute);
            gatewaySpec.put("serviceName", "{{ Global.STAGE_ID }}-" + meta.getAppId() + "-" + meta.getMicroServiceId() + ".{{ Global.NAMESPACE_ID }}");
            gatewayTrait.put("name", "gateway.trait.abm.io");
            gatewayTrait.put("runtime", "post");
            gatewayTrait.put("spec", gatewaySpec);
            if (launchObject.getGatewayRouteOrder() != null) {
                gatewaySpec.put("order", launchObject.getGatewayRouteOrder());
            }
            if (launchObject.getGatewayAuthEnabled() != null) {
                gatewaySpec.put("authEnabled", launchObject.getGatewayAuthEnabled());
            }

            traits.add(gatewayTrait);
        }

        if (launchObject.getReplicas() != null) {

            /**
             *         - name: REPLICAS
             *           value: 2
             *           toFieldPaths:
             *             - spec.replicas
             */
            JSONObject replicaValueObject = new JSONObject();
            JSONArray toFieldPaths = new JSONArray();
            toFieldPaths.add("spec.replicas");
            replicaValueObject.put("name", "REPLICAS");
            replicaValueObject.put("value", launchObject.getReplicas());
            replicaValueObject.put("toFieldPaths", toFieldPaths);
            parameterValues.add(replicaValueObject);
        }

        if (StringUtils.isNotBlank(launchObject.getTimezone())) {
            /**
             *         - name: timezoneSync.trait.abm.io
             *           runtime: pre
             *           spec:
             *             timezone: Asia/Shanghai
             */

            JSONObject timezoneTrait = new JSONObject();
            JSONObject timezoneSpec = new JSONObject();

            timezoneSpec.put("timezone", launchObject.getTimezone());
            timezoneTrait.put("name", "timezoneSync.trait.abm.io");
            timezoneTrait.put("runtime", "pre");
            timezoneTrait.put("spec", timezoneSpec);

            traits.add(timezoneTrait);
        }

        if (launchObject.getPodLabels() != null) {
            /**
             * name: podPatch.trait.abm.io
             * runtime: pre
             * spec:
             *     metadata:
             *       labels:
             *         a: b
             *       annotations:
             *         c: d
             */
            JSONObject podPatchTrait = new JSONObject();
            JSONObject podPatchSpec = new JSONObject();
            JSONObject podMetadata = new JSONObject();

            podPatchTrait.put("name", "podPatch.trait.abm.io");
            podPatchTrait.put("runtime", "pre");
            if (launchObject.getPodLabels() != null) {
                podMetadata.put("labels", launchObject.getPodLabels());
            }
            podPatchSpec.put("metadata", podMetadata);
            podPatchTrait.put("spec", podPatchSpec);
            traits.add(podPatchTrait);
        }

        JSONObject traitEnvMapList = new JSONObject();
        traitEnvMapList.put("APP_INSTANCE_ID", "{{ spec.labels[\"labels.appmanager.oam.dev/appInstanceId\"] }}");
        JSONArray traitEnvList = new JSONArray();

        for (String env : meta.getEnvKeyList()) {
            if (StringUtils.isBlank(env)) {
                continue;
            }
            String[] kv = env.split("=");
            String key = kv[0];
            if (traitEnvMapList.containsKey(key)) {
                traitEnvList.add(key);
            }
            if (kv.length > 1) {
                JSONObject valueObject = new JSONObject();
                valueObject.put("name", "Global." + key);
                valueObject.put("value", kv[1]);
                parameterValues.add(valueObject);
            }
        }

        if (traitEnvList.size() > 0) {
            JSONObject envTrait = new JSONObject();
            JSONArray dataOutputs = new JSONArray();
            envTrait.put("dataInputs", new JSONArray());
            envTrait.put("name", "systemEnv.trait.abm.io");
            envTrait.put("runtime", "pre");
            envTrait.put("spec", new JSONObject());
            for (int i = 0; i < traitEnvList.size(); i++) {
                String envKey = traitEnvList.getString(i);
                String envMapValue = traitEnvMapList.getString(envKey);
                JSONObject dataOutputObject = new JSONObject();
                dataOutputObject.put("fieldPath", envMapValue);
                dataOutputObject.put("name", "Global." + envKey);
                dataOutputs.add(dataOutputObject);
            }
            envTrait.put("dataOutputs", dataOutputs);
            traits.add(envTrait);
        }

        String systemTypeId = new DeployConfigTypeId(ComponentTypeEnum.RESOURCE_ADDON, "system-env@system-env").toString();

        List<DeployConfigDO> configs = deployConfigService.list(
                DeployConfigQueryCondition.builder()
                        .appId(meta.getAppId())
                        .typeId(systemTypeId)
                        .envId("")
                        .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                        .enabled(true)
                        .isolateNamespaceId(meta.getNamespaceId())
                        .isolateStageId(meta.getStageId())
                        .build()
        );

        // 如果存在system-env则直接进行依赖
        // todo: 判断自身的变量在system-env中有才进行依赖
        if (configs.size() > 0) {
            JSONArray dependencies = new JSONArray();
            JSONObject componentSystem = new JSONObject();
            componentSystem.put("component", "RESOURCE_ADDON|system-env@system-env");
            dependencies.add(componentSystem);
            configObject.put("dependencies", dependencies);
        }

        configObject.put("parameterValues", parameterValues);
        configObject.put("traits", traits);
        configObject.put("scopes", scopes);

        Yaml yaml = SchemaUtil.createYaml(JSONObject.class);
        String typeId = new DeployConfigTypeId(ComponentTypeEnum.K8S_MICROSERVICE, meta.getMicroServiceId()).toString();
        String metaNamespaceId = meta.getNamespaceId();
        String metaStageId = meta.getStageId();
        // TODO: FOR SREWORKS ONLY TEMPORARY
        if (EnvUtil.isSreworks()) {
            metaNamespaceId = EnvUtil.defaultNamespaceId();
            metaStageId = EnvUtil.defaultStageId();
        }
        deployConfigService.update(DeployConfigUpdateReq.builder()
                .apiVersion(DefaultConstant.API_VERSION_V1_ALPHA2)
                .appId(meta.getAppId())
                .typeId(typeId)
                .envId("")
                .inherit(false)
                .config(yaml.dumpAsMap(configObject))
                .isolateNamespaceId(metaNamespaceId)
                .isolateStageId(metaStageId)
                .build());
    }
}
