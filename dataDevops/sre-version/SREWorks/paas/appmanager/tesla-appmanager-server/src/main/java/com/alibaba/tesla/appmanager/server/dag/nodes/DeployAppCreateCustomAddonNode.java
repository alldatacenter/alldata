package com.alibaba.tesla.appmanager.server.dag.nodes;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.*;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.ParameterValueSetPolicy;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.ObjectUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName;
import com.alibaba.tesla.appmanager.domain.core.CustomWorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.CustomAddonSchema;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.server.addon.AddonManager;
import com.alibaba.tesla.appmanager.server.addon.CustomAddonInstanceManager;
import com.alibaba.tesla.appmanager.server.addon.req.ApplyCustomAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.addon.res.ApplyCustomAddonInstanceRes;
import com.alibaba.tesla.dag.common.BeanUtil;
import com.alibaba.tesla.dag.local.AbstractLocalNodeBase;
import com.alibaba.tesla.dag.model.domain.dagnode.DagInstNodeRunRet;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Arrays;
import java.util.Map;

import static com.alibaba.tesla.appmanager.server.dag.helper.DeployAppHelper.*;

/**
 * @ClassName: DeployAppCreateCustomAddonNode
 * @Author: dyj
 * @DATE: 2020-11-11
 * @Description:
 **/
@Slf4j
public class DeployAppCreateCustomAddonNode extends AbstractLocalNodeBase {

    public static Integer runTimeout = 3600;

    /**
     * 针对custom addon，用于附加AddonAttrs 用于附加唯一标识信息。
     *
     * @param appId
     * @return
     */
    private Map<String, String> getCustomAddonAttrs(String appId) {
        return ImmutableMap.of(
                "appId", appId
        );
    }

    @Override
    public DagInstNodeRunRet run() throws Exception {
        CustomAddonInstanceManager customAddonInstanceManager = BeanUtil.getBean(CustomAddonInstanceManager.class);
        ObjectUtil.checkNull(CheckNullObject.builder()
                .actionName("DeployAppCreateCustomAddonNode")
                .objectName("customAddonInstanceManager")
                .checkObject(customAddonInstanceManager)
                .build());
        AddonManager addonManager = BeanUtil.getBean(AddonManager.class);
        ObjectUtil.checkNull(CheckNullObject.builder()
                .actionName("DeployAppCreateCustomAddonNode")
                .objectName("addonManager")
                .checkObject(addonManager)
                .build());

        // 获取流程中的数据信息
        Long deployAppId = Long.valueOf(globalVariable.getString(AppFlowVariableKey.DEPLOY_ID));
        String nodeId = fatherNodeId;
        String appId = globalVariable.getString(AppFlowVariableKey.APP_ID);
        if (StringUtils.isEmpty(nodeId) || StringUtils.isEmpty(appId)) {
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                    String.format("actionName=%s|nodeId=%s|appId=%s| nodeId or appId can not be null!",
                            "DeployAppCreateCustomAddonNode", nodeId, appId));
        }

        DeployAppRevisionName revisionName = DeployAppRevisionName.valueOf(nodeId);
        ComponentTypeEnum componentType = revisionName.getComponentType();
        if (!componentType.equals(ComponentTypeEnum.CUSTOM_ADDON)) {
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                    String.format("actionName=%s|componentType=%s| componentType is not equals CUSTOM_ADDON!",
                            "DeployAppCreateCustomAddonNode", componentType.name()));
        }

        // 获取当前的 component 及 addonSchema，准备填充参数数据
        DeployAppSchema configuration = SchemaUtil.toSchema(DeployAppSchema.class,
                globalVariable.get(AppFlowVariableKey.CONFIGURATION).toString());

        // 获取全局的覆盖参数，并叠加当前的 parameterValues, 并使用该 Map 对部署单中的 component 进行渲染
        JSONObject parameters = (JSONObject) globalParams
                .getJSONObject(AppFlowParamKey.OVERWRITE_PARAMETER_VALUES).clone();
        DeployAppSchema.SpecComponent component = findComponent(nodeId, configuration);
        if (component == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    "cannot find suitable component options by nodeId " + nodeId);
        }
        for (DeployAppSchema.ParameterValue parameterValue : component.getParameterValues()) {
            String key = parameterValue.getName();
            Object value = parameterValue.getValue();
            recursiveSetParameters(parameters, null, Arrays.asList(key.split("\\.")), value,
                    ParameterValueSetPolicy.OVERWRITE_ON_CONFILICT);
        }
        recursiveSetParameters(parameters, null, Arrays.asList(CustomComponentConstant.APP_ID_PATH.split("\\.")), globalVariable.getString(AppFlowVariableKey.APP_ID),
                ParameterValueSetPolicy.OVERWRITE_ON_CONFILICT);
        recursiveSetParameters(parameters, null, Arrays.asList(CustomComponentConstant.NAMESPACE_PATH.split("\\.")), component.getNamespaceId(),
                ParameterValueSetPolicy.OVERWRITE_ON_CONFILICT);
        component = renderDeployAppComponent(parameters, component);

        // 获取 custom addon schema
        String addonId = revisionName.addonId();
        String addonName = revisionName.addonName();
        String addonVersion = revisionName.getVersion();
        CustomAddonSchema addonSchema = addonManager.getCustomAddonSchema(addonId, addonVersion);

        // 渲染 appId 和 nameSpace
        CustomWorkloadResource workload = renderCustomWorkloadMeta(parameters, addonSchema);

        // 将 ParameterValues 和 dataInputs 赋值至 woekresource.spec 的 parameterValue
        DocumentContext workloadContext = JsonPath.parse(JSONObject.toJSONString(workload));
        findParameterValues(globalParams, component)
                .forEach(item -> item.getToFieldPaths()
                        .forEach(toFieldPath -> {
                            String logSuffix = String.format("name=%s|value=%s|toFieldPath=%s|deployId=%d|componentId=%s",
                                    item.getName(), item.getValue(), toFieldPath, deployAppId, nodeId);
                            try {
                                workloadContext.set(DefaultConstant.JSONPATH_PREFIX + toFieldPath, item.getValue());
                                log.info("set dataInput value success|{}", logSuffix);
                            } catch (Exception e) {
                                log.warn("set dataInput value failed|{}|exception={}",
                                        logSuffix, ExceptionUtils.getStackTrace(e));
                                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                                        "set parameter value failed|" + logSuffix, e);
                            }
                        }));
        workload = JSONObject.parseObject(workloadContext.jsonString(),
                CustomWorkloadResource.class);
        DeployAppSchema applicationConfig = workload.getSpec().getApplicationConfig();
        DocumentContext applicationContext = JsonPath.parse(JSONObject.toJSONString(applicationConfig));
        // 将 workresource 的 pv 渲染至 applicationConfig 中的 pv
        workload.getSpec().getParameterValues().forEach(
                item -> item.getFieldPath().forEach(
                        fieldPath -> {
                            String logSuffix = String.format("name=%s|value=%s|fieldPath=%s|deployId=%d|componentId=%s",
                                    item.getName(), item.getValue(), fieldPath, deployAppId, nodeId);
                            try {
                                if (item.getValue() == null) {
                                    applicationContext.set(DefaultConstant.JSONPATH_PREFIX + fieldPath, item.getDefaultValue());
                                } else {
                                    applicationContext.set(DefaultConstant.JSONPATH_PREFIX + fieldPath, item.getValue());
                                }
                                log.info("set custom applicationConfig parameterValue success|{}", logSuffix);
                            } catch (Exception e) {
                                log.warn("set dataInput value failed|{}|exception={}",
                                        logSuffix, ExceptionUtils.getStackTrace(e));
                                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                                        "set parameter value failed|" + logSuffix, e);
                            }
                        }
                )
        );
        workload.getSpec().setApplicationConfig(
                JSONObject.parseObject(applicationContext.jsonString(), DeployAppSchema.class));
        addonSchema.getSpec().setWorkload(workload);
        System.out.println("send to custom task!!!");
        System.out.println(SchemaUtil.toYamlMapStr(addonSchema));

        // 提交 custom addon 申请任务
        String namespaceId = component.getNamespaceId();
        ObjectUtil.checkNull(CheckNullObject.builder()
                .actionName(this.getClass().getSimpleName())
                .objectName("namespaceId")
                .checkObject(namespaceId)
                .build());
        ApplyCustomAddonInstanceReq request = ApplyCustomAddonInstanceReq.builder()
                .namespaceId(namespaceId)
                .addonId(addonId)
                .creator(globalVariable.get(AppFlowVariableKey.CREATOR).toString())
                .addonVersion(addonVersion)
                .addonName(addonName)
                .addonAttrs(getCustomAddonAttrs(appId))
                .customAddonSchema(addonSchema)
                .build();
        ApplyCustomAddonInstanceRes addonInstanceTask = customAddonInstanceManager.applyCustomAddonInstance(request);
        if (addonInstanceTask.isReady()) {
            globalParams.put(AppFlowParamKey.ADDON_INSTANCE_ID, addonInstanceTask.getCustomAddonInstanceId());
        } else {
            globalParams.put(AppFlowParamKey.ADDON_INSTANCE_TASK_ID, addonInstanceTask.getCustomAddonInstanceTaskId());
        }
        return DagInstNodeRunRet.builder().build();
    }
}
