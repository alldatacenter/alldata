package com.alibaba.tesla.appmanager.server.dag.nodes;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.AppFlowParamKey;
import com.alibaba.tesla.appmanager.common.constants.AppFlowVariableKey;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.AddonInstanceTaskStatusEnum;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.ParameterValueSetPolicy;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.server.addon.AddonInstanceManager;
import com.alibaba.tesla.appmanager.server.addon.AddonManager;
import com.alibaba.tesla.appmanager.server.addon.task.AddonInstanceTaskService;
import com.alibaba.tesla.appmanager.server.dag.helper.DeployAppHelper;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceTaskDO;
import com.alibaba.tesla.dag.common.BeanUtil;
import com.alibaba.tesla.dag.local.AbstractLocalNodeBase;
import com.alibaba.tesla.dag.model.domain.dagnode.DagInstNodeRunRet;
import com.google.common.base.Enums;
import com.hubspot.jinjava.Jinjava;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部署 App - 等待 Trait 的 Addon Instance 申请完成
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class DeployAppWaitAddonNode extends AbstractLocalNodeBase {

    public static Integer runTimeout = 3600;

    private static final int MAX_WAIT_TIMES = 7200;

    private static final int WAIT_MILLIS = 500;

    @Override
    public DagInstNodeRunRet run() throws Exception {
        AddonManager addonManager = BeanUtil.getBean(AddonManager.class);
        AddonInstanceTaskService addonInstanceTaskService = BeanUtil.getBean(AddonInstanceTaskService.class);
        AddonInstanceManager addonInstanceManager = BeanUtil.getBean(AddonInstanceManager.class);
        assert addonManager != null && addonInstanceTaskService != null && addonInstanceManager != null;

        // 进行等待，最终获取实际的 addon instance
        String nodeId = fatherNodeId;
        Long deployAppId = globalVariable.getLong(AppFlowVariableKey.DEPLOY_ID);
        assert !StringUtils.isEmpty(nodeId);
        log.info("enter the execution process of DeployAppWaitAddonNode|deployAppId={}|" +
                "nodeId={}|dagInstId={}", deployAppId, nodeId, dagInstId);
        AddonInstanceDO addonInstance = waitForAvailableAddonInstance();
        log.info("deploy addon has finished running|deployAppId={}|nodeId={}|dagInstId={}|cost={}",
                deployAppId, nodeId, dagInstId, addonInstance.costTime());

        // componentType 变量提取
        DeployAppRevisionName revisionName = DeployAppRevisionName.valueOf(nodeId);
        ComponentTypeEnum componentType = revisionName.getComponentType();

        // 寻找 dataOutput 列表，并将对应的变量的值 set 到当前的部署单中
        Jinjava jinjava = new Jinjava();
        ComponentSchema addonSchema = SchemaUtil.toSchema(ComponentSchema.class, addonInstance.getAddonExt());
        JSONObject workload = (JSONObject) JSONObject.toJSON(addonSchema.getSpec().getWorkload());
        JSONObject parameters = globalParams.getJSONObject(AppFlowParamKey.OVERWRITE_PARAMETER_VALUES);
        List<DeployAppSchema.DataOutput> dataOutputs = getAddonDataOutputs(componentType);
        for (DeployAppSchema.DataOutput dataOutput : dataOutputs) {
            String fieldPath = dataOutput.getFieldPath();
            String name = dataOutput.getName();
            Object value;
            try {
                if (fieldPath.startsWith("{{")) {
                    // Jinja 渲染方式
                    value = jinjava.render(fieldPath, workload);
                } else {
                    DocumentContext workloadContext = JsonPath.parse(JSONObject.toJSONString(workload));
                    value = workloadContext.read(DefaultConstant.JSONPATH_PREFIX + fieldPath);
                }
            } catch (Exception e) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("cannot fetch dataOutput in workload|fieldPath=%s|name=%s|exception=%s|" +
                                "workload=%s", fieldPath, name, ExceptionUtils.getStackTrace(e),
                                JSONObject.toJSONString(workload)));
            }
            DeployAppHelper.recursiveSetParameters(parameters, null, Arrays.asList(name.split("\\.")), value,
                    ParameterValueSetPolicy.OVERWRITE_ON_CONFILICT);
            log.info("dataOutput has put into overwrite parameters|name={}|value={}|deployId={}|fieldPath={}",
                    name, value, deployAppId, fieldPath);
        }

        log.info("deploy addon waiting process has finished|deployAppId={}|nodeId={}|addonInstanceId={}|" +
                "dagInstId={}", deployAppId, nodeId, addonInstance.getAddonInstanceId(), dagInstId);
        return DagInstNodeRunRet.builder().build();
    }

    /**
     * 获取当前部署单的当前 Addon 的 DataOutputs 数据列表
     *
     * @param componentType 组件类型
     * @return DataOutput 列表
     */
    private List<DeployAppSchema.DataOutput> getAddonDataOutputs(ComponentTypeEnum componentType) {
        DeployAppSchema configuration = SchemaUtil.toSchema(DeployAppSchema.class,
                globalVariable.get(AppFlowVariableKey.CONFIGURATION).toString());
        String nodeId = fatherNodeId;
        assert !StringUtils.isEmpty(nodeId);

        // 根据 componentType 类型在部署单中寻找对应的 DataOutputs 配置
        switch (componentType) {
            case INTERNAL_ADDON:
            case RESOURCE_ADDON:
            case CUSTOM_ADDON:
            case K8S_MICROSERVICE:
            case K8S_JOB: {
                for (DeployAppSchema.SpecComponent component : configuration.getSpec().getComponents()) {
                    String componentId = component.getUniqueId();
                    if (nodeId.equals(componentId)) {
                        return component.getDataOutputs();
                    }
                }
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("cannot find specified nodeId %s in components/addons", nodeId));
            }
            case TRAIT_ADDON: {
                for (DeployAppSchema.SpecComponent component : configuration.getSpec().getComponents()) {
                    String componentId = component.getUniqueId();
                    DeployAppRevisionName componentRevision = DeployAppRevisionName.valueOf(componentId);
                    for (DeployAppSchema.SpecComponentTrait trait : component.getTraits()) {
                        String traitId = trait.getUniqueId(componentRevision);
                        if (traitId.equals(nodeId)) {
                            return trait.getDataOutputs();
                        }
                    }
                }
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("cannot find specified nodeId %s in traits", nodeId));
            }
            default:
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("invalid componentType %s", componentType.toString()));
        }
    }

    /**
     * 如果当前存在的是 addon instance id，那么直接查询对应的 DB 对象并返回
     * <p>
     * 如果当前存在的是 addon instance task id，那么等待该任务执行完毕，然后查询对应的实例对象并返回
     *
     * @return AddonInstanceDO
     */
    private AddonInstanceDO waitForAvailableAddonInstance() throws InterruptedException {
        AddonInstanceManager addonInstanceManager = BeanUtil.getBean(AddonInstanceManager.class);
        AddonInstanceTaskService addonInstanceTaskService = BeanUtil.getBean(AddonInstanceTaskService.class);
        assert addonInstanceManager != null && addonInstanceTaskService != null;

        // 如果已经存在 addonInstanceId，那么直接查询对应的实例
        String nodeId = fatherNodeId;
        String logSuffix = String.format("|dagInstId=%d|revisionName=%s", dagInstId, nodeId);
        String addonInstanceId = globalParams.getString(AppFlowParamKey.ADDON_INSTANCE_ID);
        if (!StringUtils.isEmpty(addonInstanceId)) {
            return addonInstanceManager.getByCondition(AddonInstanceQueryCondition.builder()
                    .addonInstanceId(addonInstanceId)
                    .build());
        }

        // 否则获取 addonInstanceTaskId 轮询等待任务完成
        long addonInstanceTaskId = Long.parseLong(globalParams.getString(AppFlowParamKey.ADDON_INSTANCE_TASK_ID));
        for (int i = 0; i < MAX_WAIT_TIMES; i++) {
            AddonInstanceTaskDO result = addonInstanceTaskService.query(addonInstanceTaskId);
            if (result == null) {
                throw new AppException(AppErrorCode.DEPLOY_ERROR,
                        "cannot find specified addon instance task record" + logSuffix);
            }
            AddonInstanceTaskStatusEnum status = Enums
                    .getIfPresent(AddonInstanceTaskStatusEnum.class, result.getTaskStatus()).orNull();
            if (status == null) {
                throw new AppException(AppErrorCode.DEPLOY_ERROR,
                        String.format("cannot parse addon instance task status %s|%s",
                                result.getTaskStatus(), logSuffix));
            }
            switch (status) {
                case EXCEPTION:
                case FAILURE:
                case WAIT_FOR_OP:
                    throw new AppException(AppErrorCode.DEPLOY_ERROR,
                            String.format("deploy component %s failed|status=%s|addonInstanceTaskId=%d|errorMessage=%s",
                                    nodeId, status, addonInstanceTaskId, result.getTaskErrorMessage()));
                case SUCCESS:
                    log.info("deploy addon {} success{}", nodeId, logSuffix);
                    return addonInstanceManager.getByCondition(AddonInstanceQueryCondition.builder()
                            .namespaceId(result.getNamespaceId())
                            .addonId(result.getAddonId())
                            .addonName(result.getAddonName())
                            .addonAttrs(JSONObject.parseObject(result.getAddonAttrs())
                                    .entrySet()
                                    .stream()
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            entry -> entry.getValue().toString()
                                    )))
                            .build());
                default:
                    break;
            }
            log.debug("running now, sleep and check again" + logSuffix);
            try {
                Thread.sleep(WAIT_MILLIS);
            } catch (InterruptedException ignored) {
                log.info("sleep interrupted, check again");
            }
        }
        throw new AppException(AppErrorCode.DEPLOY_ERROR, String.format("deploy addon %s timeout|" +
                "addonInstanceTaskId=%d", nodeId, addonInstanceTaskId));
    }
}
