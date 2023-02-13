package com.alibaba.tesla.appmanager.server.dag.nodes;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.AppFlowParamKey;
import com.alibaba.tesla.appmanager.common.constants.AppFlowVariableKey;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.*;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.InstanceIdUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.server.dag.helper.DeployAppHelper;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployComponentEvent;
import com.alibaba.tesla.appmanager.server.repository.condition.RtComponentInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceDO;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployComponentService;
import com.alibaba.tesla.appmanager.server.service.rtcomponentinstance.RtComponentInstanceService;
import com.alibaba.tesla.appmanager.spring.config.ApplicationEventPublisherProvider;
import com.alibaba.tesla.dag.common.BeanUtil;
import com.alibaba.tesla.dag.local.AbstractLocalNodeBase;
import com.alibaba.tesla.dag.model.domain.dagnode.DagInstNodeRunRet;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 部署 App - 创建 Component 部署单节点
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class DeployAppCreateComponentNode extends AbstractLocalNodeBase {

    public static Integer runTimeout = 3600;

    @Override
    public DagInstNodeRunRet run() throws Exception {
        ApplicationEventPublisherProvider publisherProvider = BeanUtil.getBean(ApplicationEventPublisherProvider.class);
        DeployAppService deployAppService = BeanUtil.getBean(DeployAppService.class);
        DeployComponentService deployComponentService = BeanUtil.getBean(DeployComponentService.class);
        RtComponentInstanceService rtComponentInstanceService = BeanUtil.getBean(RtComponentInstanceService.class);
        assert deployComponentService != null && publisherProvider != null && rtComponentInstanceService != null;
        ApplicationEventPublisher publisher = publisherProvider.getApplicationEventPublisher();

        // 解析当前 component 的 revisionName 中包含的信息
        String nodeId = fatherNodeId;
        assert !StringUtils.isEmpty(nodeId);
        DeployAppRevisionName revisionName = DeployAppRevisionName.valueOf(nodeId);
        Long deployAppId = Long.valueOf(globalVariable.getString(AppFlowVariableKey.DEPLOY_ID));
        log.info("enter the execution process of DeployAppCreateComponentNode|deployAppId={}|nodeId={}|dagInstId={}",
                deployAppId, nodeId, dagInstId);

        // 根据上面解析出来的 revisionName 中包含的信息寻找对应的 componentPackage 并得到 componentPackageId 主键
        String appId = globalVariable.getString(AppFlowVariableKey.APP_ID);
        DeployAppSchema configuration = SchemaUtil.toSchema(DeployAppSchema.class,
                globalVariable.get(AppFlowVariableKey.CONFIGURATION).toString());
        String componentRevisionKey = AppFlowParamKey.componentSchemaMapKeyGenerator(revisionName.revisionName());
        String componentSchemaStr = deployAppService.getAttr(deployAppId, componentRevisionKey);
        if (StringUtils.isEmpty(componentSchemaStr)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot get component schema with revision %s in system", revisionName.revisionName()));
        }
        DeployAppSchema.SpecComponent componentOptions = DeployAppHelper.findComponent(nodeId, configuration);
        if (componentOptions == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    "cannot find suitable component options by nodeId " + nodeId);
        }

        // 获取全局的覆盖参数，并叠加当前的 parameterValues, 并使用该 Map 对部署单中的 component 进行渲染
        JSONObject parameters = (JSONObject) globalParams
                .getJSONObject(AppFlowParamKey.OVERWRITE_PARAMETER_VALUES).clone();
        for (DeployAppSchema.ParameterValue parameterValue : componentOptions.getParameterValues()) {
            String key = parameterValue.getName();
            Object value = parameterValue.getValue();
            if (StringUtils.isEmpty(key)) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        "the parameter 'name' in parameterValues cannot be empty");
            }
            DeployAppHelper.recursiveSetParameters(parameters, null, Arrays.asList(key.split("\\.")), value,
                    ParameterValueSetPolicy.OVERWRITE_ON_CONFILICT);
        }
        componentOptions = DeployAppHelper.renderDeployAppComponent(parameters, componentOptions);

        // 将 dataInputs 中声明的变量获取过来，并赋值到 component schema 中的对应字段
        ComponentSchema componentSchema = DeployAppHelper
                .renderComponentSchemaWorkload(configuration, parameters, componentSchemaStr);

        // 开始 workload 对象内容寻找及替换
        WorkloadResource workload = componentSchema.getSpec().getWorkload();
        DeployAppHelper.setWorkloadMetaData(workload, componentOptions, appId, revisionName.getComponentName());
        DocumentContext workloadContext = JsonPath.parse(JSONObject.toJSONString(workload));
        for (DeployAppSchema.DataInput dataInput : componentOptions.getDataInputs()) {
            String key = dataInput.getValueFrom().getDataOutputName();
            Object value = DeployAppHelper.recursiveGetParameter(parameters, Arrays.asList(key.split("\\.")));
            for (String toFieldPath : dataInput.getToFieldPaths()) {
                try {
                    workloadContext.set(DefaultConstant.JSONPATH_PREFIX + toFieldPath, value);
                    log.info("set dataInput value success|key={}|value={}|toFieldPath={}|nodeId={}|" +
                            "deployAppId={}", key, value, toFieldPath, nodeId, deployAppId);
                } catch (PathNotFoundException e) {
                    log.warn("set dataInput value failed because of path not found|key={}|value={}|toFieldPath={}|" +
                                    "nodeId={}|deployAppId={}|parameters={}", key, value, toFieldPath,
                            nodeId, deployAppId, parameters.toJSONString());
                } catch (Exception e) {
                    log.warn("set dataInput value failed|key={}|value={}|toFieldPath={}|nodeId={}|" +
                                    "deployAppId={}|parameters={}|exception={}", key, value, toFieldPath,
                            nodeId, deployAppId, parameters.toJSONString(), ExceptionUtils.getStackTrace(e));
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS, "set dataInput value failed", e);
                }
            }
        }

        // 将 parameterValues 中声明的变量获取过来，并赋值到 component schema 中对应的字段
        for (DeployAppSchema.ParameterValue parameterValue : componentOptions.getParameterValues()) {
            String key = parameterValue.getName();
            Object value = parameterValue.getValue();
            if (StringUtils.isEmpty(key)) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        "the parameter 'name' in parameterValues cannot be empty");
            }
            for (String toFieldPath : parameterValue.getToFieldPaths()) {
                try {
                    workloadContext.set(DefaultConstant.JSONPATH_PREFIX + toFieldPath, value);
                    log.info("set parameter value success|key={}|value={}|toFieldPath={}|nodeId={}|" +
                            "deployAppId={}", key, value, toFieldPath, nodeId, deployAppId);
                } catch (PathNotFoundException e) {
                    log.warn("set parameter value failed because of path not found|key={}|value={}|toFieldPath={}|" +
                                    "nodeId={}|deployAppId={}", key, value, toFieldPath, nodeId, deployAppId);
                } catch (Exception e) {
                    log.warn("set parameter value failed|key={}|value={}|toFieldPath={}|nodeId={}|" +
                                    "deployAppId={}|exception={}", key, value, toFieldPath, nodeId, deployAppId,
                            ExceptionUtils.getStackTrace(e));
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS, "set parameter value failed", e);
                }
            }
        }

        // 获取基本部署目标定位信息
        String namespaceId = componentOptions.getNamespaceId();
        if (StringUtils.isEmpty(namespaceId)) {
            throw new AppException(AppErrorCode.DEPLOY_ERROR, String.format("cannot find namespaceId in " +
                    "component %s deployment configuration", nodeId));
        }
        String clusterId = componentOptions.getClusterId();
        String stageId = componentOptions.getStageId();

        // 检测当前是否已经存在组件实例，存在则获取 componentInstanceId；否则创建对应的组件实例并获取 componentInstanceId
        RtComponentInstanceQueryCondition componentInstanceCondition = RtComponentInstanceQueryCondition.builder()
                .appId(globalVariable.getString(AppFlowVariableKey.APP_ID))
                .componentType(revisionName.getComponentType().toString())
                .componentName(revisionName.getComponentName())
                .clusterId(clusterId)
                .namespaceId(namespaceId)
                .stageId(stageId)
                .build();
        String appInstanceId = InstanceIdUtil.genAppInstanceId(appId, clusterId, namespaceId, stageId);
        RtComponentInstanceDO componentInstance = rtComponentInstanceService
                .getOrCreate(componentInstanceCondition, appInstanceId, revisionName.getVersion());
        String componentInstanceId = componentInstance.getComponentInstanceId();

        // 生成 component 部署记录
        Date now = new Date();
        DeployComponentDO record = DeployComponentDO.builder()
                .deployId(deployAppId)
                .deployType(DeployComponentTypeEnum.COMPONENT.toString())
                .identifier(componentOptions.getRevisionName())
                .appId(globalVariable.getString(AppFlowVariableKey.APP_ID))
                .clusterId(clusterId)
                .namespaceId(namespaceId)
                .stageId(stageId)
                .gmtStart(now)
                .deployStatus(DeployComponentStateEnum.CREATED.toString())
                .deployCreator(globalVariable.getString(AppFlowVariableKey.CREATOR))
                .build();
        Map<DeployComponentAttrTypeEnum, String> attrMap = new HashMap<>();
        attrMap.put(DeployComponentAttrTypeEnum.OPTIONS, SchemaUtil.toYamlMapStr(componentOptions));
        deployComponentService.create(record, attrMap);
        Long deployComponentId = record.getId();
        componentSchema.getSpec()
                .setWorkload(JSONObject.parseObject(workloadContext.jsonString(), WorkloadResource.class));

        // 基础变量渲染并保存
        componentSchemaStr = SchemaUtil.toYamlMapStr(componentSchema)
                .replaceAll("PLACEHOLDER_DEPLOY_APP_ID", "deployment-" + deployAppId)
                .replaceAll("PLACEHOLDER_DEPLOY_COMPONENT_ID", "deployment-" + deployComponentId)
                .replaceAll("PLACEHOLDER_APP_INSTANCE_ID", appInstanceId)
                .replaceAll("PLACEHOLDER_COMPONENT_INSTANCE_ID", componentInstanceId);
        deployComponentService
                .updateAttr(deployComponentId, DeployComponentAttrTypeEnum.COMPONENT_SCHEMA, componentSchemaStr);
        String ownerReference = globalVariable.getString(AppFlowVariableKey.OWNER_REFERENCE);
        if (StringUtils.isEmpty(ownerReference)) {
            ownerReference = "";
        }
        deployComponentService
                .updateAttr(deployComponentId, DeployComponentAttrTypeEnum.OWNER_REFERENCES, ownerReference);
        log.info("deploy component order has created|deployAppId={}|deployComponentId={}|clusterId={}|namespaceId={}|" +
                        "stageId={}|creator={}|appInstanceId={}|componentInstanceId={}", deployAppId, deployComponentId,
                clusterId, namespaceId, stageId, creator, appInstanceId, componentInstanceId);

        // 发送 Component Deploy 工单 START 消息，触发执行
        publisher.publishEvent(new DeployComponentEvent(this, DeployComponentEventEnum.START, record.getId()));
        globalParams.put(AppFlowParamKey.DEPLOY_COMPONENT_ID, record.getId());
        globalParams.put(AppFlowParamKey.COMPONENT_INSTANCE_ID, componentInstanceId);
        return DagInstNodeRunRet.builder().build();
    }
}
