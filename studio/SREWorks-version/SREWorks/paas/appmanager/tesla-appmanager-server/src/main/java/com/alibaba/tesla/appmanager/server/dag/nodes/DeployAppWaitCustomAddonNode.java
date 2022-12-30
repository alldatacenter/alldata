package com.alibaba.tesla.appmanager.server.dag.nodes;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.AppFlowParamKey;
import com.alibaba.tesla.appmanager.common.constants.AppFlowVariableKey;
import com.alibaba.tesla.appmanager.common.constants.CheckNullObject;
import com.alibaba.tesla.appmanager.common.enums.*;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.ObjectConvertUtil;
import com.alibaba.tesla.appmanager.common.util.ObjectUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName;
import com.alibaba.tesla.appmanager.domain.schema.CustomAddonSchema;
import com.alibaba.tesla.appmanager.domain.schema.CustomAddonWorkloadSpec;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.server.addon.AddonInstanceManager;
import com.alibaba.tesla.appmanager.server.addon.AddonManager;
import com.alibaba.tesla.appmanager.server.addon.CustomAddonInstanceManager;
import com.alibaba.tesla.appmanager.server.addon.task.CustomAddonInstanceTaskService;
import com.alibaba.tesla.appmanager.server.addon.utils.AddonInstanceIdGenUtil;
import com.alibaba.tesla.appmanager.server.dag.helper.DeployAppHelper;
import com.alibaba.tesla.appmanager.server.repository.CustomAddonInstanceRepository;
import com.alibaba.tesla.appmanager.server.repository.CustomAddonInstanceTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.CustomAddonInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import com.alibaba.tesla.appmanager.server.service.deploy.business.DeployAppBO;
import com.alibaba.tesla.dag.common.BeanUtil;
import com.alibaba.tesla.dag.local.AbstractLocalNodeBase;
import com.alibaba.tesla.dag.model.domain.dagnode.DagInstNodeRunRet;
import com.google.common.base.Enums;
import com.hubspot.jinjava.Jinjava;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.alibaba.tesla.appmanager.common.constants.AppFlowParamKey.OVERWRITE_PARAMETER_VALUES;

/**
 * @ClassName: DeployAppWaitCustomAddonNode
 * @Author: dyj
 * @DATE: 2020-11-26
 * @Description:
 **/
@Slf4j
public class DeployAppWaitCustomAddonNode extends AbstractLocalNodeBase {

    public static Integer runTimeout = 3600;

    // 最大等待时间
    public static final int MAX_WAIT_TIMES = 7200;

    // 每次查询后静默时间
    public static final int WAIT_MILLIS = 500;

    @Override
    public DagInstNodeRunRet run() throws Exception {
        AddonManager addonManager = BeanUtil.getBean(AddonManager.class);
        CustomAddonInstanceTaskService customAddonInstanceTaskService = BeanUtil.getBean(
                CustomAddonInstanceTaskService.class);
        AddonInstanceManager addonInstanceManager = BeanUtil.getBean(AddonInstanceManager.class);
        ObjectUtil.checkNullList(
                Arrays.asList(CheckNullObject.builder().actionName(this.getClass().getSimpleName()).checkObject(
                                        addonManager).objectName("addonManager")
                                .build(),
                        CheckNullObject.builder().actionName(this.getClass().getSimpleName()).checkObject(
                                        customAddonInstanceTaskService).objectName("customAddonInstanceTaskService")
                                .build(),
                        CheckNullObject.builder().actionName(this.getClass().getSimpleName()).checkObject(addonInstanceManager)
                                .objectName("addonInstanceManager")
                                .build()));

        //获取实际的addon instance
        String nodeId = fatherNodeId;
        Long deployId = globalVariable.getLong(AppFlowVariableKey.DEPLOY_ID);
        ObjectUtil.checkNull(CheckNullObject.builder().actionName(this.getClass().getSimpleName()).objectName("nodeId")
                .checkObject(nodeId).build());
        log.info("waiting for {} to finished|dagInstId={}", nodeId, dagInstId);
        CustomAddonInstanceDO customAddonInstance = waitForAvailableCustomAddonInstance();

        // componentType 变量提取
        DeployAppRevisionName revisionName = DeployAppRevisionName.valueOf(nodeId);
        ComponentTypeEnum componentType = revisionName.getComponentType();

        // 查找 dataOutput 列表，并将对应的变量的值 set 到当前部署单
        Jinjava jinjava = new Jinjava();
        CustomAddonSchema customAddonSchema = SchemaUtil.toSchema(CustomAddonSchema.class,
                customAddonInstance.getAddonExt());
        JSONObject workload = (JSONObject) JSONObject.toJSON(customAddonSchema.getSpec().getWorkload());
        JSONObject parameters = globalParams.getJSONObject(OVERWRITE_PARAMETER_VALUES);
        List<DeployAppSchema.DataOutput> dataOutputs = getCustomAddonDataOutputs();
        for (DeployAppSchema.DataOutput dataOutput : dataOutputs) {
            String fieldPath = dataOutput.getFieldPath();
            String name = dataOutput.getName();
            String value = jinjava.render(fieldPath, workload);
            DeployAppHelper.recursiveSetParameters(parameters, null, Arrays.asList(name.split("\\.")), value,
                    ParameterValueSetPolicy.OVERWRITE_ON_CONFILICT);
            log.info("dataOutput has put into overwrite parameters|name={}|value={}|deployId={}|fieldPath={}",
                    name, value, deployId, fieldPath);
        }

        log.info("deploy custom addon {} success|addonInstanceId={}|dagInstId={}|revisionName={}",
                nodeId, customAddonInstance.getAddonInstanceId(), dagInstId, nodeId);

        return DagInstNodeRunRet.builder().build();
    }

    /**
     * 等待 custom addon 部署结束
     *
     * @return
     */
    private CustomAddonInstanceDO waitForAvailableCustomAddonInstance() {
        CustomAddonInstanceManager customAddonInstanceManager = BeanUtil.getBean(CustomAddonInstanceManager.class);
        CustomAddonInstanceTaskService customAddonInstanceTaskService = BeanUtil.getBean(
                CustomAddonInstanceTaskService.class);
        ObjectUtil.checkNullList(Arrays.asList(CheckNullObject.builder().actionName(
                                "waitForAvailableCustomAddonInstance").checkObject(customAddonInstanceManager)
                        .objectName("customAddonInstanceManager")
                        .build(),
                CheckNullObject.builder().actionName("waitForAvailableCustomAddonInstance").checkObject(
                                customAddonInstanceTaskService).objectName("customAddonInstanceTaskService")
                        .build()));

        // 如果已经存在 addonInstanceId，那么直接查询对应的实例
        String nodeId = fatherNodeId;
        String logSuffix = String.format("|dagInstId=%d|revisionName=%s", dagInstId, nodeId);
        String addonInstanceId = globalParams.getString(AppFlowParamKey.ADDON_INSTANCE_ID);
        if (!StringUtils.isEmpty(addonInstanceId)) {
            return customAddonInstanceManager.getByCondition(CustomAddonInstanceQueryCondition.builder()
                    .addonInstanceId(addonInstanceId)
                    .build());
        }

        // 否则获取 customAddonInstanceTaskId 轮询等待任务完成
        long addonInstanceTaskId = Long.parseLong(globalParams.getString(AppFlowParamKey.ADDON_INSTANCE_TASK_ID));
        for (int i = 0; i < MAX_WAIT_TIMES; i++) {
            CustomAddonInstanceTaskDO instanceTask = customAddonInstanceTaskService.query(addonInstanceTaskId);
            if (instanceTask == null) {
                throw new AppException(AppErrorCode.DEPLOY_ERROR,
                        "cannot find specified addon instance task record" + logSuffix);
            }
            AddonInstanceTaskStatusEnum customInstanceTaskStatus = Enums
                    .getIfPresent(AddonInstanceTaskStatusEnum.class, instanceTask.getTaskStatus()).orNull();
            if (customInstanceTaskStatus == null) {
                throw new AppException(AppErrorCode.DEPLOY_ERROR,
                        String.format("cannot parse addon instance task status %s|%s",
                                instanceTask.getTaskStatus(), logSuffix));
            }
            switch (customInstanceTaskStatus) {
                case RUNNING:
                    updateCustomAddonInstanceTask(instanceTask);
                    break;
                case EXCEPTION:
                case FAILURE:
                case WAIT_FOR_OP:
                    throw new AppException(AppErrorCode.DEPLOY_ERROR,
                            String.format("deploy component %s status: %s", nodeId, customInstanceTaskStatus));
                case SUCCESS:
                    log.info("deploy addon {} success{}", nodeId, logSuffix);
                    return customAddonInstanceManager.getByCondition(CustomAddonInstanceQueryCondition.builder()
                            .namespaceId(instanceTask.getNamespaceId())
                            .addonId(instanceTask.getAddonId())
                            .addonVersion(instanceTask.getAddonVersion())
                            .addonName(instanceTask.getAddonName())
                            .addonAttrs(JSONObject.parseObject(instanceTask.getAddonAttrs())
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

        throw new AppException(AppErrorCode.DEPLOY_ERROR, String.format("deploy custom addon %s timeout", nodeId));
    }

    /**
     * 更新 custom addon 部署状态
     *
     * @param customAddonInstanceTask
     */
    private void updateCustomAddonInstanceTask(CustomAddonInstanceTaskDO customAddonInstanceTask) {
        DeployAppService deployAppService = BeanUtil.getBean(DeployAppService.class);
        CustomAddonInstanceTaskRepository customAddonInstanceTaskRep = BeanUtil.getBean(CustomAddonInstanceTaskRepository.class);
        CustomAddonInstanceRepository customAddonInstanceRepository = BeanUtil.getBean(CustomAddonInstanceRepository.class);

        DeployAppBO deployAppBO = deployAppService.get(customAddonInstanceTask.getDeployAppId(), true);
        DeployAppDO deployApp = deployAppBO.getOrder();
        Map<String, String> attrMap = deployAppBO.getAttrMap();
        ObjectUtil.checkNull(CheckNullObject.builder()
                .actionName("updateCustomAddonInstanceTask")
                .checkObject(deployApp)
                .objectName("deployApp")
                .build());
        DeployAppStateEnum deployAppState = Enums.getIfPresent(DeployAppStateEnum.class, deployApp.getDeployStatus())
                .orNull();
        CustomAddonInstanceTaskDO record = CustomAddonInstanceTaskDO.builder()
                .id(customAddonInstanceTask.getId())
                .build();
        if (deployAppState == null) {
            log.error(
                    "actionName=updateCustomAddonInstanceTask|customAddonTask={}|deployAppId={}:deployApp status is "
                            + "null!",
                    customAddonInstanceTask.getId(), deployApp.getId());
            record.setTaskStatus(AddonInstanceTaskStatusEnum.EXCEPTION.toString());
            customAddonInstanceTaskRep.updateByPrimaryKey(record);
            return;
        }
        switch (deployAppState) {
            case SUCCESS:
                CustomAddonInstanceDO customAddonInstanceDO = CustomAddonInstanceDO.builder()
                        .addonInstanceId(AddonInstanceIdGenUtil.genInstanceId())
                        .namespaceId(customAddonInstanceTask.getNamespaceId())
                        .addonId(customAddonInstanceTask.getAddonId())
                        .addonVersion(customAddonInstanceTask.getAddonVersion())
                        .addonName(customAddonInstanceTask.getAddonName())
                        .addonAttrs(customAddonInstanceTask.getAddonAttrs())
                        .addonExt(gennerateCustomAddonExt(customAddonInstanceTask.getTaskExt(), attrMap))
                        .build();
                customAddonInstanceRepository.insert(customAddonInstanceDO);
                log.info("actionName=insertCustomInstance|addonInstanceId={}:Insert custom instance success!!",
                        customAddonInstanceDO.getAddonInstanceId());

                record.setTaskStatus(AddonInstanceTaskStatusEnum.SUCCESS.toString());
                customAddonInstanceTaskRep.updateByPrimaryKey(record);
                break;
            case FAILURE:
                record.setTaskStatus(AddonInstanceTaskStatusEnum.FAILURE.toString());
                customAddonInstanceTaskRep.updateByPrimaryKey(record);
                break;
            case EXCEPTION:
                record.setTaskStatus(AddonInstanceTaskStatusEnum.EXCEPTION.toString());
                customAddonInstanceTaskRep.updateByPrimaryKey(record);
                break;
            case WAIT_FOR_OP:
                record.setTaskStatus(AddonInstanceTaskStatusEnum.WAIT_FOR_OP.toString());
                customAddonInstanceTaskRep.updateByPrimaryKey(record);
                break;
            default:
                break;
        }
    }

    public String gennerateCustomAddonExt(String customTaskExt, Map<String, String> attrMap) {
        CustomAddonSchema customSchema = SchemaUtil.toSchema(CustomAddonSchema.class, customTaskExt);
        JSONObject parameters = ObjectConvertUtil
                .from(attrMap.get(DeployAppAttrTypeEnum.GLOBAL_PARAMS.toString()), JSONObject.class)
                .getJSONObject(AppFlowParamKey.OVERWRITE_PARAMETER_VALUES);

        CustomAddonWorkloadSpec workloadSpec = customSchema.getSpec().getWorkload().getSpec();
        workloadSpec.setCustomOutputs(parameters);
        customSchema.getSpec().getWorkload().setSpec(workloadSpec);

        return SchemaUtil.toYamlMapStr(customSchema);
    }

    /**
     * 获取部署单中 custom addon 的dataOutputs
     *
     * @return
     */
    private List<DeployAppSchema.DataOutput> getCustomAddonDataOutputs() {
        DeployAppSchema configuration = SchemaUtil.toSchema(DeployAppSchema.class,
                globalVariable.get(AppFlowVariableKey.CONFIGURATION).toString());
        String nodeId = fatherNodeId;
        assert !StringUtils.isEmpty(nodeId);

        for (DeployAppSchema.SpecComponent component : configuration.getSpec().getComponents()) {
            String uniqueId = component.getUniqueId();
            if (nodeId.equals(uniqueId)) {
                return component.getDataOutputs();
            }
        }
        throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                String.format("cannot find specified nodeId %s in components/addons", nodeId));
    }
}
