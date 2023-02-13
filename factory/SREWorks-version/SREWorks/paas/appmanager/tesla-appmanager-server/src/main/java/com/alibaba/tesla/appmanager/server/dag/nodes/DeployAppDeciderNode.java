package com.alibaba.tesla.appmanager.server.dag.nodes;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.AppFlowParamKey;
import com.alibaba.tesla.appmanager.common.constants.AppFlowVariableKey;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.ParameterValueSetPolicy;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.common.util.StringUtil;
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.server.dag.helper.DeployAppHelper;
import com.alibaba.tesla.dag.local.AbstractLocalNodeBase;
import com.alibaba.tesla.dag.model.domain.dagnode.DagInstNodeRunRet;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * 部署 App - 决策节点 (根据不同类型，引流到不同的处理节点)
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class DeployAppDeciderNode extends AbstractLocalNodeBase {

    public static Integer runTimeout = 3600;

    @Override
    public DagInstNodeRunRet run() throws Exception {
        Long deployAppId = Long.valueOf(globalVariable.get(AppFlowVariableKey.DEPLOY_ID).toString());
        String nodeId = fatherNodeId;
        if (StringUtils.isEmpty(nodeId)) {
            throw new AppException(AppErrorCode.DEPLOY_ERROR, "empty node id");
        }
        log.info("enter the execution process of DeployAppDeciderNode|deployAppId={}|nodeId={}|dagInstId={}",
                deployAppId, nodeId, dagInstId);
        globalParams.putIfAbsent(AppFlowParamKey.OVERWRITE_PARAMETER_VALUES, new JSONObject());

        // 进行类型识别并转换
        DeployAppRevisionName revisionContainer = DeployAppRevisionName.valueOf(nodeId);
        // 对于镜像节点，直接结束当前节点的运行
        if (revisionContainer.isMirrorFlag()) {
            log.info("mirror decider node has finished running|deployAppId={}|nodeId={}|dagInstId={}",
                    deployAppId, nodeId, dagInstId);
            return DagInstNodeRunRet.builder()
                    .output(ImmutableMap.of(AppFlowParamKey.COMPONENT_TYPE, ""))
                    .build();
        }
        ComponentTypeEnum componentType = revisionContainer.getComponentType();

        // 获取全局变量，并将其置入到 OVERWRITE_PARAMETER_VALUES (每个 component/addon 节点运行的时候都会还原这份变量数据)
        JSONObject parameters = globalParams.getJSONObject(AppFlowParamKey.OVERWRITE_PARAMETER_VALUES);
        DeployAppSchema configuration = SchemaUtil.toSchema(DeployAppSchema.class,
                globalVariable.get(AppFlowVariableKey.CONFIGURATION).toString());
        for (DeployAppSchema.ParameterValue parameterValue : configuration.getSpec().getParameterValues()) {
            String name = StringUtil.globalParamName(parameterValue.getName());
            Object value = parameterValue.getValue();
            DeployAppHelper.recursiveSetParameters(parameters, null, Arrays.asList(name.split("\\.")), value,
                    ParameterValueSetPolicy.OVERWRITE_ON_CONFILICT);
        }
        log.info("decider node has finished running|deployAppId={}|componentType={}|nodeId={}|dagInstId={}",
                deployAppId, componentType.toString(), nodeId, dagInstId);
        return DagInstNodeRunRet.builder()
                .output(ImmutableMap.of(AppFlowParamKey.COMPONENT_TYPE, componentType.toString()))
                .build();
    }
}
