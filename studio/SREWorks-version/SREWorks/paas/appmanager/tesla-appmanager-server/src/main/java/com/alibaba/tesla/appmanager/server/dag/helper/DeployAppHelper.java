package com.alibaba.tesla.appmanager.server.dag.helper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.appmanager.common.constants.AppFlowParamKey;
import com.alibaba.tesla.appmanager.common.enums.ParameterValueSetPolicy;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.ImageUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.common.util.StringUtil;
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName;
import com.alibaba.tesla.appmanager.domain.core.CustomWorkloadResource;
import com.alibaba.tesla.appmanager.domain.core.ImageTar;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.appmanager.domain.schema.CustomAddonSchema;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema.Spec;
import com.alibaba.tesla.appmanager.server.factory.JinjaFactory;
import com.alibaba.tesla.appmanager.spring.util.SpringBeanUtil;
import com.hubspot.jinjava.Jinjava;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 部署 App 辅助类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class DeployAppHelper {

    /**
     * 渲染 DeployApp.SpecComponent 配置
     *
     * @param component 组件配置
     * @return 渲染后的配置数据
     */
    public static DeployAppSchema.SpecComponent renderDeployAppComponent(
            JSONObject parameters, DeployAppSchema.SpecComponent component) {
        DeployAppSchema.SpecComponent copiedComponent = SerializationUtils.clone(component);
        List<DeployAppSchema.DataOutput> tmpDataOutputs = copiedComponent.getDataOutputs();
        copiedComponent.setDataOutputs(null);

        // 通过 jinja 进行变量渲染
        Jinjava jinjava = JinjaFactory.getJinjava();
        String raw = SchemaUtil.toYamlMapStr(copiedComponent);
        String renderedComponentStr = jinjava.render(raw, parameters);
        DeployAppSchema.SpecComponent renderedComponent = SchemaUtil
                .toSchema(DeployAppSchema.SpecComponent.class, renderedComponentStr);
        renderedComponent.setDataOutputs(tmpDataOutputs);
        return renderedComponent;
    }

    /**
     * 渲染 Component Schema 中的 workload 对象
     *
     * @param configuration      全局配置 DeployAppSchema
     * @param parameters         参数字典
     * @param componentSchemaStr ComponentSchema 字符串（待渲染）
     * @return 渲染好的 ComponentSchema 对象
     */
    public static ComponentSchema renderComponentSchemaWorkload(
            DeployAppSchema configuration, JSONObject parameters, String componentSchemaStr) {
        SystemProperties systemProperties = SpringBeanUtil.getBean(SystemProperties.class);
        String dockerRegistry = systemProperties.getDockerRegistry();
        String dockerNamespace = systemProperties.getDockerNamespace();
        String renderedComponentSchemaStr = renderByJinjaStr(parameters, componentSchemaStr)
                .replaceFirst("spec: \\|", "spec: ");
        ComponentSchema componentSchema = SchemaUtil.toSchema(ComponentSchema.class, renderedComponentSchemaStr);

        // 检查镜像是否需要进行替换
        String imageTarStr = configuration.getMetadata().getAnnotations().getImageTars();
        Map<String, String> imageReplaceMap = new HashMap<>();
        if (StringUtils.isNotEmpty(imageTarStr)) {
            try {
                for (ImageTar imageTar : JSONArray.parseArray(imageTarStr, ImageTar.class)) {
                    if (StringUtils.isAnyEmpty(imageTar.getImage(), imageTar.getActualImage())) {
                        throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                                "image / actualImage are required in imageTar annotations");
                    }
                    imageReplaceMap.put(imageTar.getImage(), imageTar.getActualImage());
                }
            } catch (Exception e) {
                if (e instanceof AppException) {
                    throw e;
                }
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("invalid image tar annotations provided: %s", imageTarStr));
            }
            log.info("image replace map has generated|map={}", JSONObject.toJSONString(imageReplaceMap));
        }

        // 对 component schema 中的所有 image 对象进行替换
        List<ImageTar> componentImageTarList = componentSchema.getSpec().getImages();
        for (ImageTar componentImageTar : componentImageTarList) {
            String image = componentImageTar.getImage();
            if (StringUtils.isEmpty(image)) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        "image field cannot be empty in component schema");
            }
            String actualImage;
            if (imageReplaceMap.containsKey(image)) {
                actualImage = imageReplaceMap.get(image);
            } else {
                actualImage = ImageUtil.generateLatestImage(dockerRegistry, dockerNamespace, image);
                String actualImageTag = ImageUtil.getImageTag(actualImage);
                String name = componentImageTar.getName();
                String sha256 = componentImageTar.getSha256();
                if (StringUtils.isNotEmpty(name)
                        && StringUtils.isNotEmpty(sha256)
                        && actualImageTag.length() == 64
                        && !actualImageTag.equals(sha256)) {
                    actualImage = ImageUtil.replaceImageTag(actualImage, sha256);
                }
            }
            renderedComponentSchemaStr = renderedComponentSchemaStr.replaceAll(image, actualImage);
            log.info("replace component schema image {} to {}", image, actualImage);
        }
        return SchemaUtil.toSchema(ComponentSchema.class, renderedComponentSchemaStr);
    }

    public static JSONObject renderJsonObject(JSONObject parameters, JSONObject obj) {
        return JSONObject.parseObject(renderByJinja(parameters, obj));
    }

    public static WorkloadResource renderAddonSchemaWorkload(
            JSONObject parameters, ComponentSchema addonSchema) {
        WorkloadResource workload = addonSchema.getSpec().getWorkload();
        workload = JSONObject.parseObject(
                renderByJinja(parameters, workload), WorkloadResource.class);
        workload.setSpec(StringUtil.recursiveDecodeObject(workload.getSpec()));
        return workload;
    }

    public static CustomWorkloadResource renderCustomWorkloadMeta(JSONObject parameters, CustomAddonSchema addonSchema) {
        CustomWorkloadResource workload = addonSchema.getSpec().getWorkload();
        Spec spec = workload.getSpec().getApplicationConfig().getSpec();
        workload.getSpec().getApplicationConfig().setSpec(null);
        workload = JSONObject.parseObject(
                renderByJinja(parameters, workload), CustomWorkloadResource.class);
        workload.getSpec().getApplicationConfig().setSpec(spec);
        return workload;
    }

    public static <T extends Serializable> String renderByJinja(JSONObject parameters, T content) {
        Jinjava jinjava = new Jinjava();
        String raw = JSONObject.toJSONString(SerializationUtils.clone(content));
        return jinjava.render(raw, parameters);
    }

    public static String renderByJinjaStr(JSONObject parameters, String content) {
        Jinjava jinjava = JinjaFactory.getJinjava();
        return jinjava.render(content, parameters);
    }

    /**
     * 渲染 DeployApp.SpecComponentTrait 配置
     *
     * @param trait Trait 配置
     * @return 渲染后的配置数据
     */
    public static DeployAppSchema.SpecComponentTrait renderDeployAppTrait(
            JSONObject parameters, DeployAppSchema.SpecComponentTrait trait) {
        DeployAppSchema.SpecComponentTrait copiedTrait = SerializationUtils.clone(trait);
        List<DeployAppSchema.DataOutput> tmpDataOutputs = copiedTrait.getDataOutputs();
        copiedTrait.setDataOutputs(null);

        // 通过 jinja 进行变量渲染
        Jinjava jinjava = JinjaFactory.getJinjava();
        String renderedTraitStr = jinjava.render(SchemaUtil.toYamlMapStr(copiedTrait), parameters);
        DeployAppSchema.SpecComponentTrait renderedTrait = SchemaUtil
                .toSchema(DeployAppSchema.SpecComponentTrait.class, renderedTraitStr);
        renderedTrait.setDataOutputs(tmpDataOutputs);
        return renderedTrait;
    }

    /**
     * 获取 Deploy Schema 中的指定 component
     *
     * @param configuration Deploy Schema
     * @return component 对象, 如果找不到，返回 null
     */
    public static DeployAppSchema.SpecComponent findComponent(String nodeId, DeployAppSchema configuration) {
        // 遍历所有的 component 对象，获取数据
        for (DeployAppSchema.SpecComponent component : configuration.getSpec().getComponents()) {
            String componentId = component.getUniqueId();
            if (!nodeId.equals(componentId)) {
                continue;
            }
            return component;
        }
        return null;
    }


    /**
     * 获取 Deploy Schema 中的指定 component 及对应的 trait
     *
     * @param configuration Deploy Schema
     * @return component 和 trait 对象组合
     */
    public static ComponentTrait findComponentTrait(String nodeId, DeployAppSchema configuration) {
        // 遍历所有的 trait 对象，获取数据
        for (DeployAppSchema.SpecComponent component : configuration.getSpec().getComponents()) {
            String componentId = component.getUniqueId();
            DeployAppRevisionName componentRevision = DeployAppRevisionName.valueOf(componentId);
            for (DeployAppSchema.SpecComponentTrait trait : component.getTraits()) {
                String traitId = trait.getUniqueId(componentRevision);
                if (!traitId.equals(nodeId)) {
                    continue;
                }
                return ComponentTrait.builder().component(component).trait(trait).build();
            }
        }
        throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                String.format("unable to find suitable trait to deploy|nodeId=%s", nodeId));
    }

    /**
     * 提取当前 component 中所有被设置的 parameter values，包括 dataInputs 和 parameterValues 中手工设置的两类变量
     *
     * @param component Component 对象
     * @return ParameterValue 列表
     */
    public static List<DeployAppSchema.ParameterValue> findParameterValues(
            JSONObject globalParams, DeployAppSchema.SpecComponent component) {
        List<DeployAppSchema.ParameterValue> result = new ArrayList<>();
        JSONObject parameters = globalParams.getJSONObject(AppFlowParamKey.OVERWRITE_PARAMETER_VALUES);

        // 将 dataInputs 中列出的对象添加到返回结果中
        result.addAll(component.getDataInputs().stream()
                .map(item -> DeployAppSchema.ParameterValue.builder()
                        .name(item.getValueFrom().getDataOutputName())
                        .value(recursiveGetParameter(parameters,
                                Arrays.asList(item.getValueFrom().getDataOutputName().split("\\."))))
                        .toFieldPaths(item.getToFieldPaths())
                        .build()
                ).collect(Collectors.toList()));

        // 将 parameterValues 中列出的数据添加到返回结果中
        result.addAll(component.getParameterValues().stream()
                .map(item -> DeployAppSchema.ParameterValue.builder()
                        .name(item.getName())
                        .value(item.getValue())
                        .toFieldPaths(item.getToFieldPaths())
                        .build()
                ).collect(Collectors.toList()));
        return result;
    }

    /**
     * 提取当前 trait 中所有被设置的 parameter values，包括 dataInputs 和在 spec 中手工设置的两类变量
     *
     * @param trait trait 对象
     * @return ParameterValue 列表
     */
    public static List<DeployAppSchema.ParameterValue> findParameterValues(
            JSONObject globalParams, DeployAppSchema.SpecComponentTrait trait) {
        List<DeployAppSchema.ParameterValue> result = new ArrayList<>();
        JSONObject parameters = globalParams.getJSONObject(AppFlowParamKey.OVERWRITE_PARAMETER_VALUES);

        // 将 dataInputs 中列出的对象添加到返回结果中
        result.addAll(trait.getDataInputs().stream()
                .map(item -> DeployAppSchema.ParameterValue.builder()
                        .name(item.getValueFrom().getDataOutputName())
                        .value(recursiveGetParameter(parameters,
                                Arrays.asList(item.getValueFrom().getDataOutputName().split("\\."))))
                        .toFieldPaths(item.getToFieldPaths())
                        .build()
                ).collect(Collectors.toList()));

        // 将 parameterValues 中列出的数据添加到返回结果中
        result.addAll(trait.getSpec().entrySet().stream()
                .map(item -> DeployAppSchema.ParameterValue.builder()
                        .name(item.getKey())
                        .value(item.getValue().toString())
                        .toFieldPaths(Collections.singletonList(String.format("spec.%s", item.getKey())))
                        .build()
                ).collect(Collectors.toList()));
        return result;
    }

    /**
     * 递归参数设置
     *
     * @param parameters 需要设置的参数字典
     * @param nameList   名称列表
     * @param value      值
     */
    public static void recursiveSetParameters(
            JSONObject parameters, String prefix, List<String> nameList, Object value, ParameterValueSetPolicy policy) {
        assert nameList.size() > 0;
        if (nameList.size() == 1) {
            String name = nameList.get(0);
            String actualName = StringUtils.isEmpty(prefix) ? name : prefix + "." + name;
            if (parameters.containsKey(name)) {
                switch (policy) {
                    case IGNORE_ON_CONFLICT:
                        log.info("overwrite parameter values, name={}, value={}", actualName, value);
                        return;
                    case EXCEPTION_ON_CONFILICT:
                        throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                                String.format("conflict overwrite parameter values, name=%s, value=%s", actualName, value));
                    default:
                        break;
                }
            }
            try {
                Yaml yaml = SchemaUtil.createYaml(Arrays.asList(
                        JSONObject.class, JSONArray.class, JSON.class, Object.class));
                if (value instanceof JSONObject) {
                    Object putValue = yaml.load(renderByJinjaStr(parameters, SchemaUtil.toYamlMapStr(value)));
                    parameters.put(nameList.get(0), putValue);
                } else if (value instanceof JSONArray) {
                    Object putValue = yaml.load(renderByJinjaStr(parameters, SchemaUtil.toYamlStr(value)));
                    parameters.put(nameList.get(0), putValue);
                } else {
                    parameters.put(nameList.get(0), value);
                }
            } catch (Exception e) {
                if (value instanceof String) {
                    parameters.put(nameList.get(0), renderByJinjaStr(parameters, (String) value));
                } else {
                    parameters.put(nameList.get(0), value);
                }
            }
            return;
        }

        String name = nameList.get(0);
        String nextPrefix = StringUtils.isEmpty(prefix) ? name : prefix + "." + name;
        parameters.putIfAbsent(name, new JSONObject());
        recursiveSetParameters(
                parameters.getJSONObject(name),
                nextPrefix,
                nameList.subList(1, nameList.size()),
                value,
                policy
        );
    }

    /**
     * 递归参数获取
     *
     * @param parameters 参数字典
     * @param nameList   名称列表
     * @return 实际的值
     */
    public static Object recursiveGetParameter(JSONObject parameters, List<String> nameList) {
        assert nameList.size() > 0;
        if (nameList.size() == 1) {
            return parameters.get(nameList.get(0));
        }
        String name = nameList.get(0);
        JSONObject subParameters = parameters.getJSONObject(name);
        if (subParameters == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot recursive get parameter, name=%s, parameters=%s",
                            name, parameters.toJSONString()));
        }
        return recursiveGetParameter(subParameters, nameList.subList(1, nameList.size()));
    }

    /**
     * 设置 WorkloadResource 的 MetaData 数据
     *
     * @param workload      Workload 资源实例
     * @param component     SpecComponent 配置，位于 ApplicationConfiguration 中
     * @param appId         应用 ID
     * @param componentName 组件名称
     */
    public static void setWorkloadMetaData(
            WorkloadResource workload, DeployAppSchema.SpecComponent component, String appId, String componentName) {
        // namespace
        String namespaceId = component.getNamespaceId();
        assert !StringUtils.isEmpty(namespaceId);
        workload.getMetadata().setNamespace(namespaceId);

        // stageId && name
        String stageId = component.getStageId();
        String name;
        if (StringUtils.isEmpty(stageId)) {
            name = String.format("%s-%s", appId, componentName);
        } else {
            name = String.format("%s-%s-%s", stageId, appId, componentName);
        }
        workload.getMetadata().setName(name);
    }

    /**
     * 获取组件的实际 meta name
     *
     * @param component     SpecComponent 配置，位于 ApplicationConfiguration 中
     * @param appId         应用 ID
     * @param componentName 组件名称
     * @return meta name
     */
    public static String getMetaName(DeployAppSchema.SpecComponent component, String appId, String componentName) {
        String stageId = component.getStageId();
        String name;
        if (StringUtils.isEmpty(stageId)) {
            name = String.format("%s-%s", appId, componentName);
        } else {
            name = String.format("%s-%s-%s", stageId, appId, componentName);
        }
        return name;
    }
}
