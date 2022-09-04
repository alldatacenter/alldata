package com.alibaba.tesla.appmanager.server.dag.nodes;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.AppFlowParamKey;
import com.alibaba.tesla.appmanager.common.constants.AppFlowVariableKey;
import com.alibaba.tesla.appmanager.common.enums.DeployComponentAttrTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployComponentStateEnum;
import com.alibaba.tesla.appmanager.common.enums.ParameterValueSetPolicy;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.server.dag.helper.DeployAppHelper;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployComponentService;
import com.alibaba.tesla.appmanager.server.service.deploy.business.DeployComponentBO;
import com.alibaba.tesla.dag.common.BeanUtil;
import com.alibaba.tesla.dag.local.AbstractLocalNodeBase;
import com.alibaba.tesla.dag.model.domain.dagnode.DagInstNodeRunRet;
import com.google.common.base.Enums;
import com.hubspot.jinjava.Jinjava;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部署 App - 等待 Component 部署完成节点
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class DeployAppWaitComponentNode extends AbstractLocalNodeBase {

    public static Integer runTimeout = 3600;

    private static final int MAX_WAIT_TIMES = 7200;

    private static final int WAIT_MILLIS = 500;

    @Override
    public DagInstNodeRunRet run() throws Exception {
        DeployComponentService deployComponentService = BeanUtil.getBean(DeployComponentService.class);
        assert deployComponentService != null;

        String nodeId = fatherNodeId;
        assert !StringUtils.isEmpty(nodeId);
        Long deployAppId = globalVariable.getLong(AppFlowVariableKey.DEPLOY_ID);
        long deployComponentId = Long.parseLong(globalParams.get(AppFlowParamKey.DEPLOY_COMPONENT_ID).toString());
        log.info("enter the execution process of DeployAppWaitComponentNode|deployAppId={}|deployComponentId={}|" +
                "nodeId={}|dagInstId={}", deployAppId, deployComponentId, nodeId, dagInstId);
        DeployComponentBO deployComponentBO = waitForAvailableComponent(deployAppId, deployComponentId);
        log.info("deploy component has finished running|deployAppId={}|deployComponentId={}|nodeId={}|" +
                        "dagInstId={}|cost={}", deployAppId, deployComponentId, nodeId, dagInstId,
                deployComponentBO.getSubOrder().costTime());

        // 寻找 dataOutput 列表，并将对应的变量的值 set 到当前的部署单中
        Jinjava jinjava = new Jinjava();
        ComponentSchema componentSchema = SchemaUtil
                .toSchema(ComponentSchema.class,
                        deployComponentBO.getAttrMap().get(DeployComponentAttrTypeEnum.COMPONENT_SCHEMA.toString()));
        JSONObject workload = (JSONObject) JSONObject.toJSON(componentSchema.getSpec().getWorkload());
        JSONObject parameters = globalParams.getJSONObject(AppFlowParamKey.OVERWRITE_PARAMETER_VALUES);
        List<DeployAppSchema.DataOutput> dataOutputs = getComponentDataOutputs();
        Map<String, Object> dataOutputMap = new HashMap<>();
        for (DeployAppSchema.DataOutput dataOutput : dataOutputs) {
            String fieldPath = dataOutput.getFieldPath();
            String name = dataOutput.getName();
            String value = jinjava.render(fieldPath, workload);
            dataOutputMap.put(name, value);
            DeployAppHelper.recursiveSetParameters(parameters, null, Arrays.asList(name.split("\\.")), value,
                    ParameterValueSetPolicy.OVERWRITE_ON_CONFILICT);
            log.info("dataOutput has put into overwrite parameters|name={}|value={}|deployAppId={}|" +
                    "deployComponentId={}|fieldPath={}", name, value, deployAppId, deployComponentId, fieldPath);
        }

        // 更新 dataOutputs 数据
        deployComponentService.updateAttr(deployComponentId,
                DeployComponentAttrTypeEnum.DATA_OUTPUTS, JSONObject.toJSONString(dataOutputMap));
        return DagInstNodeRunRet.builder().build();
    }

    /**
     * 获取当前部署单的当前 Component 的 DataOutputs 数据列表
     *
     * @return DataOutput 列表
     */
    private List<DeployAppSchema.DataOutput> getComponentDataOutputs() {
        DeployAppSchema configuration = SchemaUtil.toSchema(DeployAppSchema.class,
                globalVariable.get(AppFlowVariableKey.CONFIGURATION).toString());
        String nodeId = fatherNodeId;
        assert !StringUtils.isEmpty(nodeId);

        for (DeployAppSchema.SpecComponent component : configuration.getSpec().getComponents()) {
            String componentId = component.getUniqueId();
            if (nodeId.equals(componentId)) {
                return component.getDataOutputs();
            }
        }
        throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                String.format("cannot find specified nodeId %s in components", nodeId));
    }

    /**
     * 等待指定的 component deploy id 对应的任务运行完成
     *
     * @param deployComponentId Component Deploy ID
     * @return 已经终态且成功的 DeployComponentDO
     */
    private DeployComponentBO waitForAvailableComponent(long deployAppId, long deployComponentId) {
        DeployComponentService deployComponentService = BeanUtil.getBean(DeployComponentService.class);
        assert deployComponentService != null;
        String nodeId = fatherNodeId;
        assert !StringUtils.isEmpty(nodeId);
        String logSuffix = String.format("|dagInstId=%d|deployAppId=%d|deployComponentId=%d|revisionName=%s",
                dagInstId, deployAppId, deployComponentId, nodeId);

        for (int i = 0; i < MAX_WAIT_TIMES; i++) {
            DeployComponentBO result = deployComponentService.get(deployComponentId, true);
            if (result == null) {
                throw new AppException(AppErrorCode.DEPLOY_ERROR,
                        "cannot find specified component package deployment record" + logSuffix);
            }
            DeployComponentStateEnum status = Enums
                    .getIfPresent(DeployComponentStateEnum.class, result.getSubOrder().getDeployStatus()).orNull();
            if (status == null) {
                throw new AppException(AppErrorCode.DEPLOY_ERROR,
                        String.format("cannot parse component package deployment status %s|%s",
                                result.getSubOrder().getDeployStatus(), logSuffix));
            }
            switch (status) {
                case EXCEPTION:
                case FAILURE:
                case WAIT_FOR_OP:
                    throw new AppException(AppErrorCode.DEPLOY_ERROR,
                            String.format("deploy component %s failed|status=%s|errorMessage=%s",
                                    nodeId, status, result.getSubOrder().getDeployErrorMessage()));
                case SUCCESS:
                    log.info("deploy component {} success|{}|subOrder={}", nodeId, logSuffix,
                            JSONObject.toJSONString(result.getSubOrder()));
                    return result;
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
        throw new AppException(AppErrorCode.DEPLOY_ERROR, String.format("deploy component %s timeout|" +
                "deployAppId=%d|deployComponentId=%d", nodeId, deployAppId, deployComponentId));
    }
}
