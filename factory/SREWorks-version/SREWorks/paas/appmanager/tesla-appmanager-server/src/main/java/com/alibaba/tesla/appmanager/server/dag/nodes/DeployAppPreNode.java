package com.alibaba.tesla.appmanager.server.dag.nodes;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.tesla.appmanager.common.constants.AppFlowParamKey;
import com.alibaba.tesla.appmanager.common.constants.AppFlowVariableKey;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.util.InstanceIdUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.server.dag.helper.DeployAppHelper;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import com.alibaba.tesla.dag.common.BeanUtil;
import com.alibaba.tesla.dag.local.AbstractLocalNodeBase;
import com.alibaba.tesla.dag.model.domain.dagnode.DagInstNodeRunRet;
import com.aliyuncs.utils.StringUtils;
import com.google.common.base.Enums;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 部署前置执行节点，用于 global params 分配
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class DeployAppPreNode extends AbstractLocalNodeBase {

    public static final String name = DeployAppPreNode.class.getSimpleName();

    @Override
    public DagInstNodeRunRet run() throws Exception {
        DeployAppService deployAppService = BeanUtil.getBean(DeployAppService.class);
        Long deployAppId = Long.valueOf(globalVariable.get(AppFlowVariableKey.DEPLOY_ID).toString());
        log.info("enter the execution process of DeployAppPreNode|deployAppId={}|nodeId={}|dagInstId={}",
                deployAppId, nodeId, dagInstId);

        String appId = globalVariable.getString(AppFlowVariableKey.APP_ID);
        String appInstanceName = globalVariable.getString(AppFlowVariableKey.APP_INSTANCE_NAME);
        DeployAppSchema configuration = SchemaUtil.toSchema(DeployAppSchema.class,
                globalVariable.get(AppFlowVariableKey.CONFIGURATION).toString());
        List<ComponentPackageDO> componentPackages = JSONArray
                .parseArray(globalVariable.getString(AppFlowVariableKey.COMPONENT_PACKAGES), ComponentPackageDO.class);
        List<String> componentRevisionList = new ArrayList<>();
        for (ComponentPackageDO item : componentPackages) {
            DeployAppRevisionName revisionName = DeployAppRevisionName.builder()
                    .componentType(Enums.getIfPresent(ComponentTypeEnum.class, item.getComponentType()).orNull())
                    .componentName(item.getComponentName())
                    .version(item.getPackageVersion())
                    .mirrorFlag(false)
                    .build();
            DeployAppSchema.SpecComponent componentOptions = DeployAppHelper
                    .findComponent(revisionName.revisionName(), configuration);
            if (componentOptions == null) {
                continue;
            }
            String clusterId = componentOptions.getClusterId();
            String namespaceId = componentOptions.getNamespaceId();
            String stageId = componentOptions.getStageId();
            if (StringUtils.isEmpty(clusterId)) {
                clusterId = "";
            }
            if (StringUtils.isEmpty(namespaceId)) {
                namespaceId = "";
            }
            if (StringUtils.isEmpty(stageId)) {
                stageId = "";
            }
            String appInstanceId = InstanceIdUtil.genAppInstanceId(appId, clusterId, namespaceId, stageId);
            String componentSchemaStr = item.getComponentSchema()
                    .replaceAll("PLACEHOLDER_CLUSTER_ID", clusterId)
                    .replaceAll("PLACEHOLDER_NAMESPACE_ID", namespaceId)
                    .replaceAll("PLACEHOLDER_STAGE_ID", stageId)
                    .replaceAll("PLACEHOLDER_APP_INSTANCE_ID", appInstanceId)
                    .replaceAll("PLACEHOLDER_APP_INSTANCE_NAME", appInstanceName)
                    .replaceAll("PLACEHOLDER_NAME", DeployAppHelper
                            .getMetaName(componentOptions, appId, revisionName.getComponentName()));
            String componentSchemaKey = AppFlowParamKey.componentSchemaMapKeyGenerator(revisionName.revisionName());
            deployAppService.updateAttr(deployAppId, componentSchemaKey, componentSchemaStr);
            componentRevisionList.add(revisionName.revisionName());
        }
        log.info("preparation for deploy app process has finished, component schema map has put into global params|" +
                "deployAppId={}|componentRevisionList={}", deployAppId, JSONArray.toJSONString(componentRevisionList));
        return DagInstNodeRunRet.builder().build();
    }
}
