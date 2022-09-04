package com.alibaba.tesla.appmanager.server.job;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.AppInstanceStatusEnum;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.DeployAppStateEnum;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.InstanceIdUtil;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.server.repository.condition.DeployAppQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.RtAppInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.RtComponentInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceDO;
import com.alibaba.tesla.appmanager.server.service.deploy.DeployAppService;
import com.alibaba.tesla.appmanager.server.service.deploy.business.DeployAppBO;
import com.alibaba.tesla.appmanager.server.service.rtappinstance.RtAppInstanceService;
import com.alibaba.tesla.appmanager.server.service.rtcomponentinstance.RtComponentInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 孤儿 Component Instance 处理 Job
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class OrphanComponentInstanceJob {

    @Autowired
    private RtAppInstanceService appInstanceService;

    @Autowired
    private RtComponentInstanceService componentInstanceService;

    @Autowired
    private DeployAppService deployAppService;

    public void run() {
        Pagination<RtComponentInstanceDO> componentInstances = componentInstanceService
                .list(RtComponentInstanceQueryCondition.builder()
                        .page(1)
                        .pageSize(DefaultConstant.UNLIMITED_PAGE_SIZE)
                        .build());
        for (RtComponentInstanceDO componentInstance : componentInstances.getItems()) {
            try {
                String appId = componentInstance.getAppId();
                String clusterId = componentInstance.getClusterId();
                String namespaceId = componentInstance.getNamespaceId();
                String stageId = componentInstance.getStageId();

                // 针对 appmeta / developmentmeta 两个特殊的 INTERNAL_ADDON component, 不进行应用实例的创建
                String componentType = componentInstance.getComponentType();
                String componentName = componentInstance.getComponentName();
                if (ComponentTypeEnum.INTERNAL_ADDON.toString().equals(componentType)
                        && ("appmeta".equals(componentName) || "developmentmeta".equals(componentName))) {
                    continue;
                }

                // 检查 app instance 是否存在，如果不存在，则创建
                Pagination<DeployAppBO> deployAppList = deployAppService.list(DeployAppQueryCondition.builder()
                        .appId(appId)
                        .clusterId(clusterId)
                        .namespaceId(namespaceId)
                        .stageId(stageId)
                        .deployStatus(DeployAppStateEnum.SUCCESS)
                        .pageSize(1)
                        .build(), false);
                String appPackageVersion = VersionUtil.buildVersion("0.0.0");
                if (deployAppList.getItems().size() > 0) {
                    appPackageVersion = deployAppList.getItems().get(0).getOrder().getPackageVersion();
                }

                String appInstanceId = InstanceIdUtil.genAppInstanceId(appId, clusterId, namespaceId, stageId);
                RtAppInstanceDO appInstance = appInstanceService.getOrCreate(RtAppInstanceQueryCondition.builder()
                        .appId(appId)
                        .clusterId(clusterId)
                        .namespaceId(namespaceId)
                        .stageId(stageId)
                        .build(), appInstanceId, "", appPackageVersion);

                // 如果当前组件的 app instance id 与之前不同，则替换为计算出来的值 (应对历史数据)
                if (!appInstanceId.equals(componentInstance.getAppInstanceId())) {
                    RtComponentInstanceDO current = componentInstanceService.get(
                            RtComponentInstanceQueryCondition.builder()
                                    .componentInstanceId(componentInstance.getComponentInstanceId())
                                    .build());
                    if (current == null) {
                        log.warn("cannot get component instance by id {} when maintain orphan component instance",
                                componentInstance.getComponentInstanceId());
                        continue;
                    }
                    current.setAppInstanceId(appInstanceId);
                    componentInstanceService.reportRaw(current);
                    log.warn("change invalid component instance, update app instance id to {} from {}|" +
                                    "componentInstanceId={}|appId={}|clusterId={}|namespaceId={}|stageId={}",
                            appInstanceId, componentInstance.getAppInstanceId(), componentInstance.getComponentInstanceId(),
                            appId, clusterId, namespaceId, stageId);
                }

                // 如果是刚刚上报创建的 app instance，直接触发对应的状态更新
                if (AppInstanceStatusEnum.PENDING.toString().equals(appInstance.getStatus())) {
                    appInstanceService.asyncTriggerStatusUpdate(appInstanceId);
                }
            } catch (Exception e) {
                log.error("cannot execute orphan component instance maintain job, skip componentInstance|" +
                                "componentInstance={}|exception={}", JSONObject.toJSONString(componentInstance),
                        ExceptionUtils.getStackTrace(e));
            }
        }
        log.info("orphan component instances have maintained");
    }
}
