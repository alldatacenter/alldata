package com.alibaba.tesla.appmanager.server.action.impl.deploy.app;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.ComponentPackageProvider;
import com.alibaba.tesla.appmanager.common.channel.enums.DeployAppPackageConstant;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.*;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.common.util.StringUtil;
import com.alibaba.tesla.appmanager.domain.container.BizAppContainer;
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.ComponentPackageTaskCreateReq;
import com.alibaba.tesla.appmanager.domain.res.componentpackage.ComponentPackageCreateRes;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.server.action.DeployAppStateAction;
import com.alibaba.tesla.appmanager.server.dag.helper.DeployAppHelper;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployAppEvent;
import com.alibaba.tesla.appmanager.server.event.loader.DeployAppStateActionLoadedEvent;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * App 部署工单 State 处理 Action - PROCESSING_COMPONENT
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service("ProcessingComponentDeployAppStateAction")
public class ProcessingComponentDeployAppStateAction implements DeployAppStateAction, ApplicationRunner {

    private static final DeployAppStateEnum STATE = DeployAppStateEnum.PROCESSING_COMPONENT;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private ComponentPackageProvider componentPackageProvider;

    @Autowired
    private DeployAppService deployAppService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        publisher.publishEvent(new DeployAppStateActionLoadedEvent(
                this, STATE.toString(), this.getClass().getSimpleName()));
    }

    /**
     * 自身逻辑处理
     *
     * @param order   部署工单
     * @param attrMap 属性字典
     */
    @Override
    public void run(DeployAppDO order, Map<String, String> attrMap) {
        String appId = order.getAppId();
        assert !StringUtils.isEmpty(appId);

        // 根据 ApplicationConfiguration 制作当前所需要的 Component Packages
        Long deployAppId = order.getId();
        String creator = order.getDeployCreator();
        DeployAppSchema configuration = SchemaUtil.toSchema(DeployAppSchema.class,
                attrMap.get(DeployAppAttrTypeEnum.APP_CONFIGURATION.toString()));

        // 获取全局参数
        JSONObject parameters = new JSONObject();
        for (DeployAppSchema.ParameterValue parameterValue : configuration.getSpec().getParameterValues()) {
            String name = StringUtil.globalParamName(parameterValue.getName());
            Object value = parameterValue.getValue();
            DeployAppHelper.recursiveSetParameters(parameters, null, Arrays.asList(name.split("\\.")), value,
                    ParameterValueSetPolicy.OVERWRITE_ON_CONFILICT);
        }

        // 发起所有 Component Package 的启动任务
        List<Long> packageTaskIdList = new ArrayList<>();
        for (DeployAppSchema.SpecComponent specComponent : configuration.getSpec().getComponents()) {
            DeployAppRevisionName revision = DeployAppRevisionName.valueOf(specComponent.getRevisionName());
            if (!revision.getComponentType().isNotAddon() || !revision.isEmptyVersion()) {
                continue;
            }

            ComponentTypeEnum componentType = revision.getComponentType();
            String componentName = revision.getComponentName();

            // 叠加 component 局部参数
            JSONObject options = (JSONObject) parameters.clone();
            for (DeployAppSchema.ParameterValue parameterValue : specComponent.getParameterValues()) {
                String name = parameterValue.getName();
                Object value = parameterValue.getValue();
                DeployAppHelper.recursiveSetParameters(options, null, Arrays.asList(name.split("\\.")), value,
                        ParameterValueSetPolicy.OVERWRITE_ON_CONFILICT);
            }
            // container 用于获取当前系统默认的隔离 namespaceId / stageId
            BizAppContainer container = BizAppContainer.valueOf(appId);
            ComponentPackageCreateRes task = componentPackageProvider.createTask(
                    ComponentPackageTaskCreateReq.builder()
                            .appId(appId)
                            .namespaceId(container.getNamespaceId())
                            .stageId(container.getStageId())
                            .componentType(componentType.toString())
                            .componentName(componentName)
                            .version(DefaultConstant.AUTO_VERSION)
                            .options(options)
                            .appPackageTaskId(0L)
                            .build(), creator);
            Long packageTaskId = task.getComponentPackageTaskId();

            // 将 task id 填入当前组件的 parameterValues 中
            specComponent.getParameterValues().add(DeployAppSchema.ParameterValue.builder()
                    .name(DeployAppPackageConstant.KEY_COMPONENT_PACKAGE_TASK_ID)
                    .value(packageTaskId.toString())
                    .build());
            packageTaskIdList.add(packageTaskId);
        }
        if (packageTaskIdList.size() == 0) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    "invalid deploy configuration, no component package task generated");
        }

        // 写回 DeployAppSchema 数据到 DB 中
        deployAppService.updateAttr(deployAppId, DeployAppAttrTypeEnum.APP_CONFIGURATION.toString(),
                SchemaUtil.toYamlMapStr(configuration));
        log.info("all component package tasks has created|deployAppId={}|componentPackageTaskIds={}|creator={}",
                deployAppId, JSONArray.toJSONString(packageTaskIdList), creator);
        publisher.publishEvent(new DeployAppEvent(this, DeployAppEventEnum.COMPONENTS_TASK_CREATED, order.getId()));
    }
}
