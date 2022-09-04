package com.alibaba.tesla.appmanager.meta.k8smicroservice.assembly;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.assembly.BaseDtoConvert;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.ContainerTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.ClassUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.dto.*;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain.K8sMicroServiceMetaDO;
import com.google.common.base.Enums;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 微应用 DTO 转换器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
public class K8sMicroServiceMetaDtoConvert extends BaseDtoConvert<K8sMicroServiceMetaDTO, K8sMicroServiceMetaDO> {

    public K8sMicroServiceMetaDtoConvert() {
        super(K8sMicroServiceMetaDTO.class, K8sMicroServiceMetaDO.class);
    }

    @Override
    public K8sMicroServiceMetaDTO to(K8sMicroServiceMetaDO meta) {
        if (meta == null) {
            return null;
        }

        K8sMicroServiceMetaDTO result = new K8sMicroServiceMetaDTO();
        ClassUtil.copy(meta, result);

        // 将 microservice ext 中的信息解析出来
        JSONObject microServiceExtJson = JSON.parseObject(meta.getMicroServiceExt());
        if (Objects.isNull(microServiceExtJson)) {
            microServiceExtJson = new JSONObject();
        }

        // key: kind (类型)
        result.setKind(microServiceExtJson.getString("kind"));

        // key: envList (环境变量列表)
        String envListString = microServiceExtJson.getString("envList");
        if (StringUtils.isNotEmpty(envListString)) {
            result.setEnvList(JSON.parseArray(envListString, EnvMetaDTO.class));
        } else {
            result.setEnvList(Collections.emptyList());
        }

        // key: containerObjectList (容器)
        String buildObjectListString = microServiceExtJson.getString("containerObjectList");
        if (StringUtils.isNotEmpty(buildObjectListString)) {
            result.setContainerObjectList(JSON.parseArray(buildObjectListString, ContainerObjectDTO.class));
            result.getContainerObjectList().forEach(ContainerObjectDTO::initRepo);
        } else {
            result.setContainerObjectList(Collections.emptyList());
        }

        // key: initContainerList (初始化容器)
        String initContainerListString = microServiceExtJson.getString("initContainerList");
        if (StringUtils.isNotEmpty(initContainerListString)) {
            result.setInitContainerList(JSON.parseArray(initContainerListString, InitContainerDTO.class));
        } else {
            result.setInitContainerList(Collections.emptyList());
        }

        // 以下为 sreworks 专用
        String envKeyListString = microServiceExtJson.getString("envKeyList");
        if (StringUtils.isNotEmpty(envKeyListString)) {
            result.setEnvKeyList(JSON.parseArray(envKeyListString, String.class));
        } else {
            result.setEnvKeyList(Collections.emptyList());
        }
        String repoString = microServiceExtJson.getString("repo");
        if (StringUtils.isNotEmpty(repoString)) {
            result.setRepoObject(JSON.parseObject(repoString, RepoDTO.class));
        }
        String imagePushString = microServiceExtJson.getString("imagePush");
        if (StringUtils.isEmpty(imagePushString)) {
            imagePushString = microServiceExtJson.getString("image");
        }
        if (StringUtils.isNotEmpty(imagePushString)) {
            result.setImagePushObject(JSON.parseObject(imagePushString, ImagePushDTO.class));
        }
        String launchObjectString = microServiceExtJson.getString("launch");
        if (StringUtils.isNotEmpty(launchObjectString)) {
            result.setLaunchObject(JSON.parseObject(launchObjectString, LaunchDTO.class));
        }
        return result;
    }

    @Override
    public K8sMicroServiceMetaDO from(K8sMicroServiceMetaDTO dto) {
        if (dto == null) {
            return null;
        }

        List<EnvMetaDTO> tempEnvList = dto.getEnvList();
        List<ContainerObjectDTO> tempContainerObjectList = dto.getContainerObjectList();
        JSONObject ext = new JSONObject();

        // key: kind
        String kind = dto.getKind();
        if (StringUtils.isEmpty(dto.getKind())) {
            kind = DefaultConstant.DEFAULT_K8S_MICROSERVICE_KIND;
        }
        ext.put("kind", kind);

        // key: envList
        if (CollectionUtils.isNotEmpty(dto.getEnvList())) {
            ext.put("envList", dto.getEnvList());
        }

        // key: containerObjectList
        if (CollectionUtils.isNotEmpty(dto.getContainerObjectList())) {
            ext.put("containerObjectList", dto.getContainerObjectList());
        }

        // key: initContainerList
        if (CollectionUtils.isNotEmpty(dto.getInitContainerList())) {
            ext.put("initContainerList", dto.getInitContainerList());
        }

        // 以下为 sreworks 专用
        if (Objects.nonNull(dto.getRepoObject())) {
            ext.put("repo", dto.getRepoObject());
            tempContainerObjectList = buildContainerObjectList(dto.getMicroServiceId(), dto.getRepoObject(), dto.getInitContainerList());
        }
        if (CollectionUtils.isNotEmpty(dto.getEnvKeyList())) {
            ext.put("envKeyList", dto.getEnvKeyList());
            tempEnvList = dto.getEnvKeyList().stream()
                    .map(envKey -> EnvMetaDTO.builder()
                            .name(envKey)
                            .defaultValue("")
                            .comment(envKey)
                            .build())
                    .collect(Collectors.toList());
        }
        if (Objects.nonNull(dto.getImagePushObject())) {
            ext.put("image", dto.getImagePushObject());
        }
        if (Objects.nonNull(dto.getLaunchObject())) {
            ext.put("launch", dto.getLaunchObject());
        }

        // 生成 microservice ext 和 options 两个字段内容
        String extStr = ext.toJSONString();
        String optionStr = createOption(dto.getComponentType(),
                kind, tempEnvList, tempContainerObjectList, dto.getImagePushObject());
        K8sMicroServiceMetaDO result = new K8sMicroServiceMetaDO();
        ClassUtil.copy(dto, result);
        result.setMicroServiceExt(extStr);
        result.setOptions(optionStr);
        return result;
    }

    /**
     * 从 options 解析出 DTO 对象
     *
     * @param body        Options (build.yaml)
     * @param appId       应用 ID
     * @param namespaceId Namespace ID
     * @param stageId     Stage ID
     * @param productId   归属产品 ID
     * @param releaseId   归属发布版本 ID
     * @return DTO 对象
     */
    public List<K8sMicroServiceMetaDTO> to(
            JSONObject body, String appId, String namespaceId, String stageId, String productId, String releaseId) {
        String componentType = body.getString("componentType");
        String componentName = body.getString("componentName");
        JSONObject options = body.getJSONObject("options");
        if (StringUtils.isAnyEmpty(componentType, componentName) || options == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    "componentType/componentName/options are required in body");
        }

        // 解析 arch 对象列表
        JSONObject archMap = options.getJSONObject("arch");
        List<JSONObject> rawOptions = new ArrayList<>();
        if (archMap == null) {
            options.put("arch", DefaultConstant.DEFAULT_ARCH);
            rawOptions.add(options);
        } else {
            for (String arch : archMap.keySet()) {
                JSONObject current = archMap.getJSONObject(arch);
                current.put("arch", arch);
                rawOptions.add(current);
            }
        }

        // 生成 DTO 对象列表
        List<K8sMicroServiceMetaDTO> results = new ArrayList<>();
        for (JSONObject rawOption : rawOptions) {
            K8sMicroServiceMetaDTO current = new K8sMicroServiceMetaDTO();

            // 基础信息
            current.setAppId(appId);
            current.setNamespaceId(namespaceId);
            current.setStageId(stageId);
            current.setMicroServiceId(componentName);
            current.setName(componentName);
            current.setDescription(componentName);
            current.setProductId(productId);
            current.setReleaseId(releaseId);
            current.setComponentType(Enums.getIfPresent(ComponentTypeEnum.class, componentType).orNull());
            if (current.getComponentType() == null) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("invalid component type in microservice %s", componentName));
            }

            // basis
            String arch = rawOption.getString("arch");
            String kind = rawOption.getString("kind");
            if (StringUtils.isEmpty(kind)) {
                kind = "Deployment";
            }
            current.setKind(kind);
            current.setArch(arch);

            // 环境变量
            JSONArray envArray = options.getJSONArray("env");
            List<EnvMetaDTO> envList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(envArray)) {
                envArray.forEach(envObject -> envList.add(EnvMetaDTO.builder()
                        .name(envObject.toString())
                        .comment(envObject.toString())
                        .defaultValue("")
                        .build()));
            }
            current.setEnvList(envList);

            // initContainers/containers/job
            JSONObject lastBuild = null;
            List<ContainerObjectDTO> containerObjectList = new ArrayList<>();
            JSONArray initContainers = rawOption.getJSONArray("initContainers");
            if (CollectionUtils.isNotEmpty(initContainers)) {
                for (int i = 0; i < initContainers.size(); i++) {
                    lastBuild = initContainers.getJSONObject(i).getJSONObject("build");
                    containerObjectList.add(
                            getContainerObjectDTO(initContainers.getJSONObject(i), ContainerTypeEnum.INIT_CONTAINER));
                }
            }
            JSONArray containers = rawOption.getJSONArray("containers");
            if (CollectionUtils.isNotEmpty(containers)) {
                for (int i = 0; i < containers.size(); i++) {
                    lastBuild = containers.getJSONObject(i).getJSONObject("build");
                    containerObjectList.add(
                            getContainerObjectDTO(containers.getJSONObject(i), ContainerTypeEnum.CONTAINER));
                }
            }
            JSONObject job = rawOption.getJSONObject("job");
            if (job != null) {
                lastBuild = job.getJSONObject("build");
                containerObjectList.add(getContainerObjectDTO(job, ContainerTypeEnum.K8S_JOB));
            }
            current.setContainerObjectList(containerObjectList);

            // imagePushObject
            if (lastBuild != null) {
                String imagePushRegistry = lastBuild.getString("imagePushRegistry");
                String dockerSecretName = lastBuild.getString("dockerSecretName");
                boolean imagePushUseBranchAsTag = lastBuild.getBooleanValue("imagePushUseBranchAsTag");
                if (StringUtils.isNotEmpty(imagePushRegistry)) {
                    String[] arr = imagePushRegistry.split("/");
                    if (arr.length != 2) {
                        throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                                "invalid imagePushRegistry in build object");
                    }
                    current.setImagePushObject(ImagePushDTO.builder()
                            .dockerSecretName(dockerSecretName)
                            .imagePushRegistry(ImagePushRegistryDTO.builder()
                                    .dockerRegistry(arr[0])
                                    .dockerNamespace(arr[1])
                                    .useBranchAsTag(imagePushUseBranchAsTag)
                                    .build())
                            .build());
                }
            }

            results.add(current);
        }
        return results;
    }

    /**
     * options 对象中的 container 对象转换为 ContainerObjectDTO 对象
     *
     * @param container     Container JSONObject 对象
     * @param containerType Container 类型
     * @return ContainerObjectDTO
     */
    private ContainerObjectDTO getContainerObjectDTO(JSONObject container, ContainerTypeEnum containerType) {
        ContainerObjectDTO containerObjectDTO = new ContainerObjectDTO();
        containerObjectDTO.setContainerType(containerType);
        containerObjectDTO.setName(container.getString("name"));

        JSONObject build = container.getJSONObject("build");
        containerObjectDTO.setRepo(build.getString("repo"));
        containerObjectDTO.setBranch(build.getString("branch"));
        containerObjectDTO.setDockerfileTemplate(build.getString("dockerfileTemplate"));

        List<ArgMetaDTO> dockerfileTemplateArgs = new ArrayList<>();
        if (MapUtils.isNotEmpty(build.getJSONObject("dockerfileTemplateArgs"))) {
            build.getJSONObject("dockerfileTemplateArgs").forEach((k, v) ->
                    dockerfileTemplateArgs.add(ArgMetaDTO.builder().name(k).value((String) v).build())
            );
        }

        containerObjectDTO.setDockerfileTemplateArgs(dockerfileTemplateArgs);
        List<ArgMetaDTO> buildArgs = new ArrayList<>();
        if (MapUtils.isNotEmpty(build.getJSONObject("args"))) {
            build.getJSONObject("args").forEach((k, v) -> buildArgs.add(ArgMetaDTO.builder().name(k)
                    .value((String) v).build())
            );
        }
        containerObjectDTO.setBuildArgs(buildArgs);
        if (CollectionUtils.isNotEmpty(container.getJSONArray("command"))) {
            containerObjectDTO.setCommand(container.getJSONArray("command").toJavaList(String.class));
        }
        return containerObjectDTO;
    }

    /**
     * SREWorks 专用: 根据 repo 对象生成 containerObjectList 和 initContainerList 对象
     *
     * @param microServiceId    微服务标识
     * @param repoDTO           repo 对象 (复用到 containerObjectList 和 initContainerList 对象)
     * @param initContainerList initContainer 精简后的 initContainer 对象内容
     * @return ContainerObjectDTO 列表对象
     */
    private static List<ContainerObjectDTO> buildContainerObjectList(
            String microServiceId, RepoDTO repoDTO, List<InitContainerDTO> initContainerList) {
        List<ContainerObjectDTO> containerObjectDTOList = new ArrayList<>();
        ContainerObjectDTO container = ContainerObjectDTO.builder()
                .containerType(ContainerTypeEnum.CONTAINER)
                .appName(microServiceId)
                .branch(DefaultConstant.DEFAULT_REPO_BRANCH)
                .name(microServiceId)
                .repo(repoDTO.getRepo())
                .repoDomain(repoDTO.getRepoDomain())
                .repoGroup(repoDTO.getRepoGroup())
                .repoType(repoDTO.getRepoType())
                .ciAccount(repoDTO.getCiAccount())
                .ciToken(repoDTO.getCiToken())
                .repoPath(repoDTO.getRepoPath())
                .build();
        if (StringUtils.isNotEmpty(repoDTO.getDockerfilePath())) {
            container.setDockerfileTemplate(repoDTO.getDockerfilePath());
        } else {
            container.setDockerfileTemplate("Dockerfile.tpl");
        }
        containerObjectDTOList.add(container);
        if (CollectionUtils.isNotEmpty(initContainerList)) {
            for (InitContainerDTO initContainerDTO : initContainerList) {
                ContainerObjectDTO initContainer = ContainerObjectDTO.builder()
                        .containerType(ContainerTypeEnum.INIT_CONTAINER)
                        .appName(microServiceId)
                        .branch(DefaultConstant.DEFAULT_REPO_BRANCH)
                        .name(initContainerDTO.createContainerName())
                        .repo(repoDTO.getRepo())
                        .repoDomain(repoDTO.getRepoDomain())
                        .repoGroup(repoDTO.getRepoGroup())
                        .repoType(repoDTO.getRepoType())
                        .ciAccount(repoDTO.getCiAccount())
                        .ciToken(repoDTO.getCiToken())
                        .repoPath(repoDTO.getRepoPath())
                        .build();
                if (StringUtils.isNotEmpty(initContainerDTO.getDockerfilePath())) {
                    initContainer.setDockerfileTemplate(initContainerDTO.getDockerfilePath());
                } else {
                    initContainer.setDockerfileTemplate(initContainerDTO.createDockerFileTemplate());
                }
                containerObjectDTOList.add(initContainer);
            }
        }
        return containerObjectDTOList;
    }

    /**
     * 创建 options 对象内容 (build.yaml)
     *
     * @param componentType          组件类型
     * @param kind                   微服务类型
     * @param envList                env 列表
     * @param containerObjectDTOList ContainerObjectDTO 对象列表
     * @param imagePushObject        imagePush 对象
     * @return options yaml (build.yaml)
     */
    public static String createOption(
            ComponentTypeEnum componentType, String kind, List<EnvMetaDTO> envList,
            List<ContainerObjectDTO> containerObjectDTOList, ImagePushDTO imagePushObject) {
        JSONObject options = new JSONObject();
        options.put("kind", kind);
        JSONArray env = new JSONArray();
        if (CollectionUtils.isNotEmpty(envList)) {
            envList.forEach(envDTO -> {
                if (StringUtils.isNotBlank(envDTO.getName())) {
                    env.add(envDTO.getName().split("=")[0]);
                }
            });
        }
        options.put("env", env);

        if (componentType == ComponentTypeEnum.K8S_MICROSERVICE || componentType == ComponentTypeEnum.K8S_JOB) {
            enrichMetaInOptions(options, componentType, containerObjectDTOList, imagePushObject);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid component type");
        }

        JSONObject result = new JSONObject();
        result.put("options", options);
        return SchemaUtil.createYaml(JSONObject.class).dumpAsMap(result);
    }

    /**
     * 将 component 对象 (componentType/containerObjectDTOList/imagePushObject 等) 组装到 options 中
     *
     * @param options                需要扩充的 options 对象
     * @param componentType          组件类型
     * @param containerObjectDTOList ContainerObjectDTO 对象列表
     * @param imagePushObject        ImagePushObject 对象
     */
    private static void enrichMetaInOptions(
            JSONObject options, ComponentTypeEnum componentType, List<ContainerObjectDTO> containerObjectDTOList,
            ImagePushDTO imagePushObject) {
        JSONArray initContainers = new JSONArray();
        JSONArray containers = new JSONArray();
        JSONObject job = new JSONObject();

        containerObjectDTOList.forEach(containerObjectDTO -> {
            JSONObject build = new JSONObject();

            build.put("repo", containerObjectDTO.getRepo());
            build.put("branch", containerObjectDTO.getBranch());
            build.put("dockerfileTemplate", containerObjectDTO.getDockerfileTemplate());
            if (StringUtils.isNotEmpty(containerObjectDTO.getCiAccount())) {
                build.put("ciAccount", containerObjectDTO.getCiAccount());
            }
            if (StringUtils.isNotEmpty(containerObjectDTO.getCiToken())) {
                build.put("ciToken", containerObjectDTO.getCiToken());
            }
            if (StringUtils.isNotEmpty(containerObjectDTO.getRepoPath())) {
                build.put("repoPath", containerObjectDTO.getRepoPath());
            }
            addImagePushProperties(build, imagePushObject);
            if (Objects.nonNull(imagePushObject) && StringUtils.isNotEmpty(imagePushObject.getDockerSecretName())) {
                build.put("dockerSecretName", imagePushObject.getDockerSecretName());
            }
            JSONObject dockerfileTemplateArgs = new JSONObject();
            if (CollectionUtils.isNotEmpty(containerObjectDTO.getDockerfileTemplateArgs())) {
                containerObjectDTO.getDockerfileTemplateArgs().forEach(
                        argDTO -> dockerfileTemplateArgs.put(argDTO.getName(), argDTO.getValue())
                );
            }
            build.put("dockerfileTemplateArgs", dockerfileTemplateArgs);
            JSONObject buildArgs = new JSONObject();
            if (CollectionUtils.isNotEmpty(containerObjectDTO.getBuildArgs())) {
                containerObjectDTO.getBuildArgs().forEach(
                        argDTO -> buildArgs.put(argDTO.getName(), argDTO.getValue()));
            }
            build.put("args", buildArgs);
            JSONObject container = new JSONObject();
            container.put("name", containerObjectDTO.getName());
            container.put("build", build);
            if (CollectionUtils.isNotEmpty(containerObjectDTO.getPorts())) {
                JSONArray ports = new JSONArray();
                containerObjectDTO.getPorts().forEach(portDTO -> {
                    JSONObject args = new JSONObject();
                    args.put(portDTO.getName(), portDTO.getValue());
                    ports.add(args);
                });
                container.put("ports", ports);
            }
            if (containerObjectDTO.getCommand() != null) {
                container.put("command", containerObjectDTO.getCommand());
            }

            if (componentType == ComponentTypeEnum.K8S_JOB) {
                job.putAll(container);
                options.put("job", job);
            } else {
                if (Objects.nonNull(containerObjectDTO.getContainerType())) {
                    switch (containerObjectDTO.getContainerType()) {
                        case INIT_CONTAINER:
                            initContainers.add(container);
                            if (CollectionUtils.isNotEmpty(initContainers)) {
                                options.put("initContainers", initContainers);
                            }
                            break;
                        case CONTAINER:
                            containers.add(container);
                            if (CollectionUtils.isNotEmpty(containers)) {
                                options.put("containers", containers);
                            }
                            break;
                    }
                }
            }
        });
    }

    /**
     * 增加 imagePush 配置
     *
     * @param build 构建对象
     */
    private static void addImagePushProperties(JSONObject build, ImagePushDTO imagePushObject) {
        if (Objects.nonNull(imagePushObject) && Objects.nonNull(imagePushObject.getImagePushRegistry())) {
            ImagePushRegistryDTO registry = imagePushObject.getImagePushRegistry();
            build.put("imagePush", true);
            build.put("imagePushRegistry",
                    String.format("%s/%s", registry.getDockerRegistry(), registry.getDockerNamespace()));
            build.put("imagePushUseBranchAsTag", registry.isUseBranchAsTag());
        }
    }
}
