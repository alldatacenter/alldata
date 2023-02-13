//package com.alibaba.tesla.appmanager.server.action.impl.deploy.component;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//import com.alibaba.tesla.appmanager.api.AddonInstanceService;
//import com.alibaba.tesla.appmanager.common.enums.DeployComponentStateEnum;
//import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
//import com.alibaba.tesla.appmanager.common.exception.AppException;
//import com.alibaba.tesla.appmanager.domain.AppPackageMeta;
//import com.alibaba.tesla.appmanager.domain.DeployComponentExtContainer;
//import com.alibaba.tesla.appmanager.domain.dto.AddonInstanceComponentRelExtDTO;
//import com.alibaba.tesla.appmanager.domain.req.AddonInstanceApplyReq;
//import com.alibaba.tesla.appmanager.server.action.DeployComponentStateAction;
//import com.alibaba.tesla.appmanager.server.event.deploy.component.ResourceInsufficientDeployComponentEvent;
//import com.alibaba.tesla.appmanager.server.event.deploy.component.ResourceSufficientDeployComponentEvent;
//import com.alibaba.tesla.appmanager.server.event.loader.DeployComponentStateActionLoadedEvent;
//import com.alibaba.tesla.appmanager.server.repository.ComponentPackageRepository;
//import com.alibaba.tesla.appmanager.server.repository.DeployComponentRepository;
//import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
//import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDO;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang.exception.ExceptionUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.ApplicationEventPublisher;
//import org.springframework.stereotype.Service;
//import org.springframework.util.StringUtils;
//import org.yaml.snakeyaml.Yaml;
//
//import javax.annotation.PostConstruct;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * Component 部署工单 State 处理 Action - CHECKING
// *
// * @author yaoxing.gyx@alibaba-inc.com
// */
//@Slf4j
//@Service("CheckingDeployComponentStateAction")
//public class CheckingDeployComponentStateAction implements DeployComponentStateAction {
//
//    private static final String LOG_PRE = String.format("action=action.%s|message=",
//        CheckingDeployComponentStateAction.class.getSimpleName());
//
//    private static final DeployComponentStateEnum STATE = DeployComponentStateEnum.CHECKING;
//
//    @Autowired
//    private ApplicationEventPublisher publisher;
//
//    @Autowired
//    private AddonInstanceService addonInstanceService;
//
//    @Autowired
//    private ComponentPackageRepository componentPackageRepository;
//
//    /**
//     * 初始化，注册自身
//     */
//    @PostConstruct
//    public void init() {
//        publisher.publishEvent(new DeployComponentStateActionLoadedEvent(
//            this, STATE.toString(), this.getClass().getSimpleName()));
//    }
//
//    /**
//     * 自身逻辑处理
//     *
//     * @param order 部署工单
//     */
//    @Override
//    public void run(DeployComponentDO order) {
//        Long componentPackageId = order.getComponentPackageId();
//        ComponentPackageDO componentPackageDO = componentPackageRepository.selectByPrimaryKey(componentPackageId);
//        if (componentPackageDO == null) {
//            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
//                String.format("cannot find specified component package|componentPackageId=%d|order=%s",
//                    componentPackageId, JSONObject.toJSONString(order)));
//        }
//
//        String namespaceId = order.getNamespaceId();
//        String envId = order.getEnvId();
//        String appId = order.getAppId();
//        String componentType = componentPackageDO.getComponentType();
//        String componentName = componentPackageDO.getComponentName();
//        String packageAddon = componentPackageDO.getPackageAddon();
//        String resourceYaml = "";
//        if (!StringUtils.isEmpty(packageAddon)) {
//            AppPackageMeta.ComponentAddon componentAddon = JSONObject
//                .parseObject(packageAddon, AppPackageMeta.ComponentAddon.class);
//            Map<String, Object> resource = new HashMap<>();
//            resource.put("addon", componentAddon);
//            Yaml yaml = SchemaUtil.createYaml();
//            resourceYaml = yaml.dump(resource);
//        }
//        AddonInstanceApplyReq addonRequest = AddonInstanceApplyReq.builder()
//            .namespaceId(namespaceId)
//            .envId(envId)
//            .appId(appId)
//            .componentType(componentType)
//            .componentName(componentName)
//            .addonResourceYml(resourceYaml)
//            .build();
//        List<AddonInstanceComponentRelExtDTO> addonInstances;
//        try {
//             addonInstances = addonInstanceService.applyInstance(addonRequest);
//        } catch (Exception e) {
//            log.warn("check component resource failed|appDeployId={}|componentDeployId={}|" +
//                "componentPackageId={}|namespaceId={}|envId={}|appId={}|componentType={}|componentName={}|" +
//                "addonResourceYaml={}|exception={}", order.getDeployId(), order.getId(), componentPackageId,
//                namespaceId, envId, appId, componentType, componentName, resourceYaml, ExceptionUtils.getStackTrace(e));
//            order.setDeployErrorMessage(e.getMessage());
//            deployComponentRepository.updateByPrimaryKey(order);
//            publisher.publishEvent(new ResourceInsufficientDeployComponentEvent(this, order.getId()));
//            return;
//        }
//        log.info("check component resource success|appDeployId={}|componentDeployId={}|" +
//            "componentPackageId={}|namespaceId={}|envId={}|appId={}|componentType={}|componentName={}|" +
//            "result={}|request={}", order.getDeployId(), order.getId(), componentPackageId,
//            namespaceId, envId, appId, componentType, componentName, JSONObject.toJSONString(addonInstances),
//            JSON.toJSONString(addonRequest));
//
//        // 资源申请成功
//        DeployComponentExtContainer extContainer = DeployComponentExtContainer.builder()
//            .addonInstances(addonInstances)
//            .build();
//        order.setDeployExt(JSONObject.toJSONString(extContainer));
//        deployComponentRepository.updateByPrimaryKey(order);
//        publisher.publishEvent(new ResourceSufficientDeployComponentEvent(this, order.getId()));
//    }
//}
