package com.alibaba.tesla.appmanager.server.action.impl.deploy.component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.AppFlowParamKey;
import com.alibaba.tesla.appmanager.common.enums.*;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName;
import com.alibaba.tesla.appmanager.domain.req.deploy.GetDeployComponentHandlerReq;
import com.alibaba.tesla.appmanager.domain.res.deploy.GetDeployComponentHandlerRes;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandlerFactory;
import com.alibaba.tesla.appmanager.server.action.DeployComponentStateAction;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployComponentEvent;
import com.alibaba.tesla.appmanager.server.event.loader.DeployComponentStateActionLoadedEvent;
import com.alibaba.tesla.appmanager.server.lib.helper.DagHelper;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDO;
import com.alibaba.tesla.appmanager.server.service.componentpackage.ComponentPackageService;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployComponentService;
import com.alibaba.tesla.appmanager.server.service.deploy.handler.DeployComponentHandler;
import com.alibaba.tesla.dag.repository.domain.DagInstDO;
import com.alibaba.tesla.dag.schedule.status.DagInstStatus;
import com.alibaba.tesla.dag.services.DagInstNewService;
import com.google.common.base.Enums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * Component 部署工单 State 处理 Action - RUNNING
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service("RunningDeployComponentStateAction")
public class RunningDeployComponentStateAction implements DeployComponentStateAction, ApplicationRunner {

    private static final DeployComponentStateEnum STATE = DeployComponentStateEnum.RUNNING;

    /**
     * 检查间隔
     */
    private static final Integer SLEEP_SECS = 10;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private DagInstNewService dagInstNewService;

    @Autowired
    private DagHelper dagHelper;

    @Autowired
    private DeployComponentService deployComponentService;

    @Autowired
    private ComponentPackageService componentPackageService;

    @Autowired
    private GroovyHandlerFactory groovyHandlerFactory;

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
        ComponentTypeEnum componentType = Enums
                .getIfPresent(ComponentTypeEnum.class, componentPackageDO.getComponentType()).orNull();
        assert componentType != null;
        String componentName = componentPackageDO.getComponentName();

        // 获取 Groovy Handler，如果获取不到，那么走传统部署逻辑
        DeployComponentHandler handler = groovyHandlerFactory.getByComponentType(DeployComponentHandler.class,
                subOrder.getAppId(), componentType, componentName, ComponentActionEnum.DEPLOY);
        if (handler == null) {
            compatibleRunner(subOrder, attrMap);
            return;
        }

        // 进行 Groovy 部署过程
        GetDeployComponentHandlerReq request = GetDeployComponentHandlerReq.builder()
                .deployAppId(subOrder.getDeployId())
                .deployComponentId(subOrder.getId())
                .appId(subOrder.getAppId())
                .clusterId(subOrder.getClusterId())
                .namespaceId(subOrder.getNamespaceId())
                .stageId(subOrder.getStageId())
                .componentType(componentPackageDO.getComponentType())
                .componentName(componentPackageDO.getComponentName())
                .attrMap(attrMap)
                .build();
        GetDeployComponentHandlerRes response;
        try {
            response = handler.get(request);
        } catch (Exception e) {
            response = GetDeployComponentHandlerRes.builder()
                    .status(DeployComponentStateEnum.WAIT_FOR_OP)
                    .message(String.format("deploy components failed|errorMessage=%s", ExceptionUtils.getStackTrace(e)))
                    .build();
        }
        switch (response.getStatus()) {
            case SUCCESS:
                log.info("component has reached success state|deployAppId={}|deployComponentId={}|" +
                                "identifier={}|message={}", subOrder.getDeployId(), subOrder.getId(),
                        subOrder.getIdentifier(), response.getMessage());
                if (response.getComponentSchema() != null) {
                    deployComponentService.updateAttr(subOrder.getId(),
                            DeployComponentAttrTypeEnum.COMPONENT_SCHEMA,
                            SchemaUtil.toYamlMapStr(response.getComponentSchema()));
                    log.info("find updated component schema when component reached success state|deployAppId={}|" +
                                    "deployComponentId={}|identifier={}|componentSchema={}", subOrder.getDeployId(),
                            subOrder.getId(), subOrder.getIdentifier(),
                            JSONObject.toJSONString(response.getComponentSchema()));
                }
                publisher.publishEvent(new DeployComponentEvent(
                        this, DeployComponentEventEnum.FLOW_SUCCEED, subOrder.getId()));
                break;
            case CREATED:
            case RUNNING:
            case PROCESSING:
                try {
                    Thread.sleep(SLEEP_SECS * 1000);
                } catch (InterruptedException ignored) {
                }
                log.info("component is still running now|deployAppId={}|deployComponentId={}|identifier={}",
                        subOrder.getDeployId(), subOrder.getId(), JSONObject.toJSONString(request));
                publisher.publishEvent(new DeployComponentEvent(
                        this, DeployComponentEventEnum.TRIGGER_UPDATE, subOrder.getId()));
                break;
            case FAILURE:
            case EXCEPTION:
            case WAIT_FOR_OP:
                log.warn("component deployment failed|deployAppId={}|deployComponentId={}|request={}|response={}",
                        subOrder.getDeployId(), subOrder.getId(), JSONObject.toJSONString(request),
                        JSONObject.toJSONString(response));
                subOrder.setDeployErrorMessage(response.getMessage());
                deployComponentService.update(subOrder);
                publisher.publishEvent(new DeployComponentEvent(
                        this, DeployComponentEventEnum.FLOW_FAILED, subOrder.getId()));
                break;
            default:
                throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                        "invalid component deployment status " + response.getStatus().toString());
        }
    }

    private void compatibleRunner(DeployComponentDO subOrder, Map<String, String> attrMap) {
        long dagInstId = Long.parseLong(subOrder.getDeployProcessId());
        DagInstDO dagInst = dagInstNewService.getDagInstById(dagInstId);
        DagInstStatus status = Enums.getIfPresent(DagInstStatus.class, dagInst.getStatus()).orNull();
        if (status == null) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, "status is null");
        }
        String logDetail = String.format("|deployAppId=%d|deployComponentId=%d|identifier=%s|dagInstId=%d|status=%s|" +
                        "creator=%s", subOrder.getDeployId(), subOrder.getId(), subOrder.getIdentifier(), dagInstId, status,
                subOrder.getDeployCreator());
        log.info("check dag status" + logDetail);
        if (!status.isEnd()) {
            return;
        }

        // 终态时进行数据打印及事件发送
        JSONObject globalParamsJson = dagInst.fetchGlobalParamsJson();
        String globalParamsJsonStr = globalParamsJson.toJSONString();
        log.info("dag global params" + logDetail + "|params={}", globalParamsJsonStr);
        if (status.equals(DagInstStatus.SUCCESS)) {
            // 成功时获取当前流程修改后的 ComponentSchema 中的 Workload Spec，为 dataOutputs 做准备
            JSONObject workloadSpec = dagInst.fetchGlobalParamsJson().getJSONObject(AppFlowParamKey.WORKLOAD_SPEC);
            if (workloadSpec != null && workloadSpec.size() > 0) {
                ComponentSchema componentSchema = SchemaUtil.toSchema(ComponentSchema.class,
                        attrMap.get(DeployComponentAttrTypeEnum.COMPONENT_SCHEMA.toString()));
                componentSchema.getSpec().getWorkload().setSpec(workloadSpec);
                deployComponentService.updateAttr(subOrder.getId(),
                        DeployComponentAttrTypeEnum.COMPONENT_SCHEMA, SchemaUtil.toYamlMapStr(componentSchema));
                log.info("replace workload spec with executor data|deployAppId={}|deployComponentId={}|dagInstId={}|" +
                                "workloadSpec={}", subOrder.getDeployId(), subOrder.getId(), dagInstId,
                        workloadSpec.toJSONString());
            }
            deployComponentService.updateAttr(subOrder.getId(),
                    DeployComponentAttrTypeEnum.GLOBAL_PARAMS, globalParamsJsonStr);
            log.info("update global params with executor data|deployAppId={}|deployComponentId={}|dagInstId={}|" +
                    "globalParams={}", subOrder.getDeployId(), subOrder.getId(), dagInstId, globalParamsJsonStr);
            publisher.publishEvent(
                    new DeployComponentEvent(this, DeployComponentEventEnum.FLOW_SUCCEED, subOrder.getId()));
        } else {
            JSONArray errorMessages = new JSONArray();
            try {
                dagHelper.collectExceptionMessages(dagInstId, errorMessages);
            } catch (Exception e) {
                log.error("show dag exception detail failed" + logDetail + "|exception={}",
                        ExceptionUtils.getStackTrace(e));
            }
            subOrder.setDeployErrorMessage(errorMessages.toJSONString());
            deployComponentService.update(subOrder);
            publisher.publishEvent(
                    new DeployComponentEvent(this, DeployComponentEventEnum.FLOW_FAILED, subOrder.getId()));
        }
    }
}
