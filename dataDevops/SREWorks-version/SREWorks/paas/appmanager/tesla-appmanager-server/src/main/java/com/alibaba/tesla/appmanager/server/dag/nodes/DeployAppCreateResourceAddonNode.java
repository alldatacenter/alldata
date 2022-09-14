package com.alibaba.tesla.appmanager.server.dag.nodes;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.AppFlowParamKey;
import com.alibaba.tesla.appmanager.common.constants.AppFlowVariableKey;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.ParameterValueSetPolicy;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.server.addon.Addon;
import com.alibaba.tesla.appmanager.server.addon.AddonInstanceManager;
import com.alibaba.tesla.appmanager.server.addon.AddonManager;
import com.alibaba.tesla.appmanager.server.addon.req.ApplyAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.addon.res.ApplyAddonInstanceRes;
import com.alibaba.tesla.appmanager.server.dag.helper.DeployAppHelper;
import com.alibaba.tesla.dag.common.BeanUtil;
import com.alibaba.tesla.dag.local.AbstractLocalNodeBase;
import com.alibaba.tesla.dag.model.domain.dagnode.DagInstNodeRunRet;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Arrays;
import java.util.Map;

import static com.alibaba.tesla.appmanager.server.dag.helper.DeployAppHelper.*;

/**
 * 部署 App - 创建 Resource Addon 节点
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class DeployAppCreateResourceAddonNode extends AbstractLocalNodeBase {

    public static Integer runTimeout = 3600;

    /**
     * 针对于 ResourceAddon，返回 AddonAttrs 用于附加唯一标识信息
     *
     * @param appId 应用 ID
     * @return map
     */
    private Map<String, String> getResourceAddonAttrs(String appId) {
        return ImmutableMap.of(
                "appId", appId
        );
    }

    @Override
    public DagInstNodeRunRet run() throws Exception {
        AddonInstanceManager addonInstanceManager = BeanUtil.getBean(AddonInstanceManager.class);
        AddonManager addonManager = BeanUtil.getBean(AddonManager.class);
        assert addonInstanceManager != null && addonManager != null;

        // 获取流程中的数据信息
        Long deployAppId = Long.valueOf(globalVariable.getString(AppFlowVariableKey.DEPLOY_ID));
        String nodeId = fatherNodeId;
        log.info("enter the execution process of DeployAppCreateResourceAddonNode|deployAppId={}|nodeId={}|" +
                "dagInstId={}", deployAppId, nodeId, dagInstId);
        String appId = globalVariable.getString(AppFlowVariableKey.APP_ID);
        assert !StringUtils.isEmpty(nodeId) && !StringUtils.isEmpty(appId);
        DeployAppRevisionName revisionName = DeployAppRevisionName.valueOf(nodeId);
        ComponentTypeEnum componentType = revisionName.getComponentType();
        assert componentType == ComponentTypeEnum.RESOURCE_ADDON;

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
        component = renderDeployAppComponent(parameters, component);

        // 获取 addon schema
        String addonId = revisionName.addonId();
        String addonName = revisionName.addonName();
        Addon addon = addonManager.getAddon(ComponentTypeEnum.RESOURCE_ADDON, addonId);
        if (addon == null) {
            throw new AppException(AppErrorCode.DEPLOY_ERROR,
                    String.format("cannot find resource addon by addonId %s", addonId));
        }
        ComponentSchema addonSchema = addon.getAddonSchema();

        // 将当前的每个参数按 toFieldPaths 赋予到对应的 schema 内容中
        WorkloadResource workload = DeployAppHelper.renderAddonSchemaWorkload(parameters, addonSchema);
        DeployAppHelper.setWorkloadMetaData(workload, component, appId, revisionName.getComponentName());
        DocumentContext workloadContext = JsonPath.parse(JSONObject.toJSONString(workload));
        findParameterValues(globalParams, component)
                .forEach(item -> item.getToFieldPaths()
                        .forEach(toFieldPath -> {
                            String logSuffix = String.format("name=%s|value=%s|toFieldPath=%s|deployId=%d|componentId=%s",
                                    item.getName(), item.getValue(), toFieldPath, deployAppId, nodeId);
                            try {
                                workloadContext.set(DefaultConstant.JSONPATH_PREFIX + toFieldPath, item.getValue());
                                log.info("set dataInput value success|{}", logSuffix);
                            } catch (PathNotFoundException e) {
                                log.warn("set dataInput value failed because of path not exist|{}|exception={}",
                                        logSuffix, ExceptionUtils.getStackTrace(e));
                            } catch (Exception e) {
                                log.warn("set dataInput value failed|{}|exception={}",
                                        logSuffix, ExceptionUtils.getStackTrace(e));
                                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                                        "set parameter value failed|" + logSuffix, e);
                            }
                        }));
        addonSchema.getSpec()
                .setWorkload(JSONObject.parseObject(workloadContext.jsonString(), WorkloadResource.class));

        // 提交 addon 申请任务
        String namespaceId = component.getNamespaceId();
        assert !StringUtils.isEmpty(namespaceId);
        ApplyAddonInstanceReq request = ApplyAddonInstanceReq.builder()
                .namespaceId(namespaceId)
                .addonId(addonId)
                .addonName(addonName)
                .addonAttrs(getResourceAddonAttrs(appId))
                .schema(addonSchema)
                .build();
        ApplyAddonInstanceRes addonInstanceTask = addonInstanceManager.applyAddonInstance(request);
        if (addonInstanceTask.isReady()) {
            globalParams.put(AppFlowParamKey.ADDON_INSTANCE_ID, addonInstanceTask.getAddonInstanceId());
        } else {
            globalParams.put(AppFlowParamKey.ADDON_INSTANCE_TASK_ID, addonInstanceTask.getAddonInstanceTaskId());
        }
        log.info("deploy resource addon task has created|deployAppId={}|namespaceId={}|addonId={}|" +
                        "addonName={}|appId={}|creator={}",
                deployAppId, namespaceId, addonId, addonName, appId, creator);
        return DagInstNodeRunRet.builder().build();
    }
}
