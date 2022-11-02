package com.alibaba.tesla.appmanager.server.action.impl.deploy.component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.autoconfig.ClusterProperties;
import com.alibaba.tesla.appmanager.common.constants.ComponentFlowVariableKey;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.*;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.ObjectConvertUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName;
import com.alibaba.tesla.appmanager.domain.core.StorageFile;
import com.alibaba.tesla.appmanager.domain.req.componentinstance.ReportRtComponentInstanceStatusReq;
import com.alibaba.tesla.appmanager.domain.req.deploy.LaunchDeployComponentHandlerReq;
import com.alibaba.tesla.appmanager.domain.res.deploy.LaunchDeployComponentHandlerRes;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandlerFactory;
import com.alibaba.tesla.appmanager.server.action.DeployComponentStateAction;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployComponentEvent;
import com.alibaba.tesla.appmanager.server.event.loader.DeployComponentStateActionLoadedEvent;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDO;
import com.alibaba.tesla.appmanager.server.service.componentpackage.ComponentPackageService;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployComponentService;
import com.alibaba.tesla.appmanager.server.service.deploy.handler.DeployComponentHandler;
import com.alibaba.tesla.appmanager.server.service.rtcomponentinstance.RtComponentInstanceService;
import com.alibaba.tesla.appmanager.server.storage.Storage;
import com.alibaba.tesla.dag.services.DagInstService;
import com.google.common.base.Enums;
import io.fabric8.kubernetes.api.model.OwnerReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Component 部署工单 State 处理 Action - PROCESSING
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service("ProcessingDeployComponentStateAction")
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class ProcessingDeployComponentStateAction implements DeployComponentStateAction, ApplicationRunner {

    private static final DeployComponentStateEnum STATE = DeployComponentStateEnum.PROCESSING;

    private static final Integer DEFAULT_EXPIRATION = 3600;

    /**
     * 默认组件触发流程名称
     */
    private static final String DAG_SUFFIX = "DEPLOYMENT_DAG";

    /**
     * 单元测试专用 DAG 图
     */

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private DagInstService dagInstService;

    @Autowired
    private DeployComponentService deployComponentService;

    @Autowired
    private Storage storage;

    @Autowired
    private ClusterProperties clusterProperties;

    @Autowired
    private GroovyHandlerFactory groovyHandlerFactory;

    @Autowired
    private ComponentPackageService componentPackageService;

    @Autowired
    private RtComponentInstanceService rtComponentInstanceService;

    // Annotations
    private static final String ANNOTATIONS_VERSION = "annotations.appmanager.oam.dev/version";
    private static final String ANNOTATIONS_COMPONENT_INSTANCE_ID = "annotations.appmanager.oam.dev/componentInstanceId";
    private static final String ANNOTATIONS_APP_INSTANCE_NAME = "annotations.appmanager.oam.dev/appInstanceName";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        publisher.publishEvent(new DeployComponentStateActionLoadedEvent(
                this, STATE.toString(), this.getClass().getSimpleName()));
    }

    /**
     * 自身逻辑处理
     *
     * @param subOrder 部署工单
     * @param attrMap  属性字典
     */
    @Override
    public void run(DeployComponentDO subOrder, Map<String, String> attrMap) {
        DeployAppRevisionName revisionName = DeployAppRevisionName.valueOf(subOrder.getIdentifier());
        ComponentPackageQueryCondition condition = ComponentPackageQueryCondition.builder()
                .appId(subOrder.getAppId())
                .componentType(revisionName.getComponentType().toString())
                .componentName(revisionName.getComponentName())
                .packageVersion(revisionName.getVersion())
                .withBlobs(false)
                .build();
        ComponentPackageDO componentPackageDO = componentPackageService.get(condition);
        if (componentPackageDO == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("deploy failed, cannot get component package|revisionName=%s|condition=%s",
                            subOrder.getIdentifier(), JSONObject.toJSONString(condition)));
        }

        // 解析包，并获取下载 URL
        String packagePath = componentPackageDO.getPackagePath();
        if (StringUtils.isEmpty(packagePath)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find package path in component package %s",
                            JSONObject.toJSONString(componentPackageDO)));
        }
        StorageFile storageFile = new StorageFile(packagePath);
        String bucketName = storageFile.getBucketName();
        String objectName = storageFile.getObjectName();
        String componentUrl = storage.getObjectUrl(bucketName, objectName, DEFAULT_EXPIRATION);

        // 获取 Component Schema
        if (attrMap.get(DeployComponentAttrTypeEnum.COMPONENT_SCHEMA.toString()) == null) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                    String.format("cannot get deploy ext info|deployAppId=%d|deployComponentId=%d",
                            subOrder.getDeployId(), subOrder.getId()));
        }
        ComponentSchema componentSchema = SchemaUtil.toSchema(ComponentSchema.class,
                attrMap.get(DeployComponentAttrTypeEnum.COMPONENT_SCHEMA.toString()));
        String componentSchemaStr = ObjectConvertUtil.toJsonString(componentSchema);

        // 获取 DeployAppSchema.SpecComponent 运行时配置
        if (attrMap.get(DeployComponentAttrTypeEnum.OPTIONS.toString()) == null) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                    String.format("cannot get deploy options info|deployAppId=%d|deployComponentId=%d",
                            subOrder.getDeployId(), subOrder.getId()));
        }
        DeployAppSchema.SpecComponent componentOptions = SchemaUtil
                .toSchema(DeployAppSchema.SpecComponent.class, attrMap.get(DeployComponentAttrTypeEnum.OPTIONS.toString()));
        String componentOptionsStr = ObjectConvertUtil.toJsonString(componentOptions);

        // 获取 owner reference 引用定位
        String ownerReference = attrMap.get(DeployComponentAttrTypeEnum.OWNER_REFERENCES.toString());

        // Groovy 处理类获取，如果获取不到，则走原始 DAG 处理逻辑
        ComponentTypeEnum componentType = Enums
                .getIfPresent(ComponentTypeEnum.class, componentPackageDO.getComponentType()).orNull();
        assert componentType != null;
        String componentName = componentPackageDO.getComponentName();
        DeployComponentHandler handler = groovyHandlerFactory.getByComponentType(DeployComponentHandler.class,
                subOrder.getAppId(), componentType, componentName, ComponentActionEnum.DEPLOY);
        if (handler == null) {
            JSONObject variables = new JSONObject();
            variables.put(DefaultConstant.DAG_TYPE, DagTypeEnum.DEPLOY_COMPONENT.toString());
            variables.put(ComponentFlowVariableKey.DEPLOY_ID, String.valueOf(subOrder.getId()));
            variables.put(ComponentFlowVariableKey.DEPLOY_APP_ID, String.valueOf(subOrder.getDeployId()));
            variables.put(ComponentFlowVariableKey.APP_ID, subOrder.getAppId());
            variables.put(ComponentFlowVariableKey.CLUSTER_ID, subOrder.getClusterId());
            variables.put(ComponentFlowVariableKey.NAMESPACE_ID, subOrder.getNamespaceId());
            variables.put(ComponentFlowVariableKey.STAGE_ID, subOrder.getStageId());
            variables.put(ComponentFlowVariableKey.COMPONENT_PACKAGE_URL, componentUrl);
            variables.put(ComponentFlowVariableKey.COMPONENT_TYPE, componentPackageDO.getComponentType());
            variables.put(ComponentFlowVariableKey.COMPONENT_NAME, componentPackageDO.getComponentName());
            variables.put(ComponentFlowVariableKey.COMPONENT_SCHEMA, componentSchemaStr);
            variables.put(ComponentFlowVariableKey.COMPONENT_OPTIONS, componentOptionsStr);
            variables.put(ComponentFlowVariableKey.IN_LOCAL_CLUSTER, clusterProperties.getLocal());
            String dagName = getDagName(componentPackageDO.getComponentType());
            log.info("prepare to start deploy component package dag|deployAppId={}|deployComponentId={}|" +
                            "identifier={}|componentSchema={}|componentOptions={}", subOrder.getDeployId(), subOrder.getId(),
                    subOrder.getIdentifier(), componentSchemaStr, componentOptionsStr);

            // 启动 Component 部署流程
            try {
                deployComponentService.updateAttr(
                        subOrder.getId(), DeployComponentAttrTypeEnum.GLOBAL_VARIABLES, variables.toJSONString());
                deployComponentService.updateAttr(subOrder.getId(), DeployComponentAttrTypeEnum.GLOBAL_PARAMS, "{}");
                Long dagInstId = dagInstService.start(dagName, variables, true);
                log.info("start deploy component package dag inst success|deployAppId={}|deployComponentId={}|" +
                        "identifier={}", subOrder.getDeployId(), subOrder.getId(), subOrder.getIdentifier());
                subOrder.setDeployProcessId(String.valueOf(dagInstId));
                deployComponentService.update(subOrder);

                // 对于老流程，直接置状态为 COMPLETED
                JSONObject annotations = (JSONObject) componentSchema
                        .getSpec()
                        .getWorkload()
                        .getMetadata()
                        .getAnnotations();
                String version = annotations.getString(ANNOTATIONS_VERSION);
                String componentInstanceId = annotations.getString(ANNOTATIONS_COMPONENT_INSTANCE_ID);
                String appInstanceName = annotations.getString(ANNOTATIONS_APP_INSTANCE_NAME);
                if (StringUtils.isNotEmpty(version)
                        && StringUtils.isNotEmpty(componentInstanceId)) {
                    rtComponentInstanceService.report(ReportRtComponentInstanceStatusReq.builder()
                            .componentInstanceId(componentInstanceId)
                            .appInstanceName(appInstanceName)
                            .clusterId(subOrder.getClusterId())
                            .namespaceId(subOrder.getNamespaceId())
                            .stageId(subOrder.getStageId())
                            .appId(subOrder.getAppId())
                            .componentType(componentPackageDO.getComponentType())
                            .componentName(componentPackageDO.getComponentName())
                            .version(version)
                            .status(ComponentInstanceStatusEnum.COMPLETED.toString())
                            .conditions(new ArrayList<>())
                            .build());
                } else {
                    log.warn("found lost annotations in component schema|version={}|" +
                                    "componentInstanceId={}|appId={}|componentType={}|componentName={}", version,
                            componentInstanceId, subOrder.getAppId(), componentPackageDO.getComponentType(),
                            componentPackageDO.getComponentName());
                }

                // 进入下一流程
                publisher.publishEvent(new DeployComponentEvent(this,
                        DeployComponentEventEnum.PROCESS_FINISHED, subOrder.getId()));
            } catch (Exception e) {
                log.error("start deploy component package dag inst failed|deployAppId={}|deployComponentId={}|" +
                                "identifier={}|variables={}|exception={}", subOrder.getDeployId(), subOrder.getId(),
                        subOrder.getIdentifier(), JSON.toJSONString(variables), ExceptionUtils.getStackTrace(e));
                subOrder.setDeployErrorMessage(e.getMessage());
                deployComponentService.update(subOrder);
                publisher.publishEvent(new DeployComponentEvent(this,
                        DeployComponentEventEnum.FLOW_FAILED, subOrder.getId()));
            }
        } else {
            LaunchDeployComponentHandlerReq request = LaunchDeployComponentHandlerReq.builder()
                    .deployAppId(subOrder.getDeployId())
                    .deployComponentId(subOrder.getId())
                    .identifier(subOrder.getIdentifier())
                    .appId(subOrder.getAppId())
                    .clusterId(subOrder.getClusterId())
                    .namespaceId(subOrder.getNamespaceId())
                    .stageId(subOrder.getStageId())
                    .componentPackageUrl(componentUrl)
                    .componentType(componentPackageDO.getComponentType())
                    .componentName(componentPackageDO.getComponentName())
                    .componentSchema(componentSchema)
                    .componentOptions(componentOptions)
                    .ownerReference(ownerReference)
                    .inLocalCluster(clusterProperties.getLocal())
                    .build();
            try {
                LaunchDeployComponentHandlerRes response = handler.launch(request);
                log.info("start deploy component process handler success|deployAppId={}|deployComponentId={}|" +
                        "identifier={}", subOrder.getDeployId(), subOrder.getId(), subOrder.getIdentifier());
                if (response.getComponentSchema() != null) {
                    deployComponentService.updateAttr(subOrder.getId(),
                            DeployComponentAttrTypeEnum.COMPONENT_SCHEMA, SchemaUtil.toYamlMapStr(response.getComponentSchema()));
                    log.info("find updated component schema when component reached processing state|deployAppId={}|" +
                                    "deployComponentId={}|identifier={}|componentSchema={}", subOrder.getDeployId(),
                            subOrder.getId(), subOrder.getIdentifier(),
                            JSONObject.toJSONString(response.getComponentSchema()));
                }
                publisher.publishEvent(new DeployComponentEvent(this,
                        DeployComponentEventEnum.PROCESS_FINISHED, subOrder.getId()));
            } catch (Exception e) {
                log.error("start deploy component process handler failed|deployAppId={}|deployComponentId={}|" +
                                "identifier={}|exception={}", subOrder.getDeployId(), subOrder.getId(),
                        subOrder.getIdentifier(), ExceptionUtils.getStackTrace(e));
                subOrder.setDeployErrorMessage(e.getMessage());
                deployComponentService.update(subOrder);
                publisher.publishEvent(new DeployComponentEvent(this,
                        DeployComponentEventEnum.FLOW_FAILED, subOrder.getId()));
            }
        }
    }

    /**
     * 根据 componentType 获取 DAG 启动名称
     *
     * @param componentType 组件类型
     * @return DAG 名称
     */
    private String getDagName(String componentType) {
        return String.join("_", Arrays.asList(componentType, DAG_SUFFIX));
    }
}
