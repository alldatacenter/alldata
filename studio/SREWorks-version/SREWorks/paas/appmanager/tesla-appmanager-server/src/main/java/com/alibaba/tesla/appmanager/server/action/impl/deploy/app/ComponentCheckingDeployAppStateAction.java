package com.alibaba.tesla.appmanager.server.action.impl.deploy.app;

import com.alibaba.tesla.appmanager.common.enums.DeployAppAttrTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployAppEventEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployAppStateEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.container.DeployAppRevisionName;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.server.action.DeployAppStateAction;
import com.alibaba.tesla.appmanager.server.event.deploy.DeployAppEvent;
import com.alibaba.tesla.appmanager.server.event.loader.DeployAppStateActionLoadedEvent;
import com.alibaba.tesla.appmanager.server.repository.AppPackageComponentRelRepository;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageComponentRelQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageComponentRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * App 部署工单 State 处理 Action - COMPONENT_CHECKING
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service("ComponentCheckingDeployAppStateAction")
public class ComponentCheckingDeployAppStateAction implements DeployAppStateAction, ApplicationRunner {

    private static final DeployAppStateEnum STATE = DeployAppStateEnum.COMPONENT_CHECKING;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private AppPackageComponentRelRepository appPackageComponentRelRepository;

    @Autowired
    private ComponentPackageRepository componentPackageRepository;

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
     * @param attrMap 扩展属性字典
     */
    @Override
    public void run(DeployAppDO order, Map<String, String> attrMap) {
        Long appPackageId = order.getAppPackageId();
        if (appPackageId == null || appPackageId == 0) {
            publisher.publishEvent(new DeployAppEvent(this, DeployAppEventEnum.COMPONENTS_NOT_FOUND, order.getId()));
            return;
        }

        // 当应用包存在时，如果 revisionName 的 version 是 _，那么对该占位符进行动态替换
        List<AppPackageComponentRelDO> rels = appPackageComponentRelRepository.selectByCondition(
                AppPackageComponentRelQueryCondition.builder().appPackageId(appPackageId).build());
        Map<String, String> componentVersionMap = new HashMap<>();
        for (AppPackageComponentRelDO rel : rels) {
            Long componentPackageId = rel.getComponentPackageId();
            assert componentPackageId != null;
            ComponentPackageDO componentPackageDO = componentPackageRepository.getByCondition(
                    ComponentPackageQueryCondition.builder()
                            .id(componentPackageId)
                            .build());
            assert componentPackageDO != null;
            String key = componentKey(componentPackageDO.getComponentType(), componentPackageDO.getComponentName());
            componentVersionMap.put(key, componentPackageDO.getPackageVersion());
        }
        DeployAppSchema configuration = SchemaUtil.toSchema(DeployAppSchema.class,
                attrMap.get(DeployAppAttrTypeEnum.APP_CONFIGURATION.toString()));
        for (DeployAppSchema.SpecComponent specComponent : configuration.getSpec().getComponents()) {
            DeployAppRevisionName revision = DeployAppRevisionName.valueOf(specComponent.getRevisionName());
            if (!revision.getComponentType().isNotAddon() || !revision.isEmptyVersion()) {
                continue;
            }
            String key = componentKey(revision.getComponentType().toString(), revision.getComponentName());
            String actualVersion = componentVersionMap.get(key);
            if (StringUtils.isEmpty(actualVersion)) {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("invalid deploy configuration, cannot find mapping component by componentType %s " +
                                "and componentName %s", revision.getComponentType().toString(), revision.getComponentName()));
            }
            String newRevisionName = DeployAppRevisionName.builder()
                    .componentType(revision.getComponentType())
                    .componentName(revision.getComponentName())
                    .version(actualVersion)
                    .build().revisionName();
            specComponent.setRevisionName(newRevisionName);
            log.info("replace revision name success|deployAppId={}|appPackageId={}|revision={}",
                    order.getId(), appPackageId, newRevisionName);
        }

        // 写回 DeployAppSchema 数据到 DB 中
        deployAppService.updateAttr(order.getId(), DeployAppAttrTypeEnum.APP_CONFIGURATION.toString(),
                SchemaUtil.toYamlMapStr(configuration));
        publisher.publishEvent(new DeployAppEvent(this, DeployAppEventEnum.COMPONENTS_EXISTS, order.getId()));
    }

    /**
     * 组装某个部署单中单个 component 的 Key, 用作 Map 中
     *
     * @param componentType ComponentType
     * @param componentName ComponentName
     * @return string
     */
    private String componentKey(String componentType, String componentName) {
        return componentType + "_" + componentName;
    }
}
