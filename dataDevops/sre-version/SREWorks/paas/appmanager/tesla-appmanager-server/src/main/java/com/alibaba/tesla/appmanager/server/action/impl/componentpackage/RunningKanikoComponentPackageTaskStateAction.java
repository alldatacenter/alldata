package com.alibaba.tesla.appmanager.server.action.impl.componentpackage;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskStateEnum;
import com.alibaba.tesla.appmanager.server.action.ComponentPackageTaskStateAction;
import com.alibaba.tesla.appmanager.server.event.componentpackage.FailedComponentPackageTaskEvent;
import com.alibaba.tesla.appmanager.server.event.loader.ComponentPackageTaskStateActionLoadedEvent;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageRepository;
import com.alibaba.tesla.appmanager.server.repository.ComponentPackageTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;
import com.alibaba.tesla.appmanager.server.service.componentpackage.ComponentPackageBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @ClassName: RunningKanikoComponentPackageTaskStateAction
 * @Author: dyj
 * @DATE: 2021-07-10
 * @Description:
 **/
@Slf4j
@Service("RunningKanikoComponentPackageTaskStateAction")
public class RunningKanikoComponentPackageTaskStateAction implements ComponentPackageTaskStateAction, ApplicationRunner {
    private static final ComponentPackageTaskStateEnum STATE = ComponentPackageTaskStateEnum.RUNNING;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private ComponentPackageTaskRepository componentPackageTaskRepository;

    @Autowired
    private ComponentPackageRepository componentPackageRepository;

    @Autowired
    private ComponentPackageBuilderService componentPackageBuilderService;

    @Autowired
    private SystemProperties systemProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (systemProperties.isEnableKaniko()) {
            publisher.publishEvent(new ComponentPackageTaskStateActionLoadedEvent(
                    this, STATE.toString(), this.getClass().getSimpleName()));
        }
    }

    /**
     * 自身逻辑处理
     *
     * @param taskDO 部署工单
     */
    @Override
    public void run(ComponentPackageTaskDO taskDO) {
        ComponentPackageTaskDO packageTaskDO = updateComponentTaskStatus(taskDO);
        Long componentPackageTaskId = packageTaskDO.getId();
        String appId = packageTaskDO.getAppId();
        String componentType = packageTaskDO.getComponentType();
        String componentName = packageTaskDO.getComponentName();
        String packageVersion = packageTaskDO.getPackageVersion();
        String packageCreator = packageTaskDO.getPackageCreator();
        String packageOptions = packageTaskDO.getPackageOptions();
        String logSuffix = String.format("|componentPackageTaskId=%d|appId=%s|componentType=%s|componentName=%s|" +
                "packageVersion=%s", componentPackageTaskId, appId, componentType, componentName, packageVersion);

        // 已经存在记录，进行校验
        ComponentPackageQueryCondition condition = ComponentPackageQueryCondition.builder()
                .appId(appId)
                .componentType(componentType)
                .componentName(componentName)
                .packageVersion(packageVersion)
                .withBlobs(true)
                .build();
        List<ComponentPackageDO> componentPackages = componentPackageRepository.selectByCondition(condition);
        if (componentPackages.size() > 0) {
            String message = "the version you submitted already exists, please choose another one";
            log.warn(message + logSuffix);
            packageTaskDO.setTaskLog(message);
            componentPackageTaskRepository.updateByCondition(packageTaskDO, ComponentPackageTaskQueryCondition.builder()
                    .id(packageTaskDO.getId())
                    .build());
            publisher.publishEvent(new FailedComponentPackageTaskEvent(this, componentPackageTaskId));
            return;
        }

        try {
            componentPackageBuilderService.kanikoBuild(packageTaskDO);
        } catch (Exception e) {
            ComponentPackageTaskQueryCondition location = ComponentPackageTaskQueryCondition
                    .builder()
                    .id(taskDO.getId())
                    .build();
            packageTaskDO = componentPackageTaskRepository.getByCondition(location);
            String tasklog = String.format("Message:%s, Cause:%s", e.getMessage(), ExceptionUtils.getStackTrace(e));
            packageTaskDO.setTaskLog(tasklog);
            int updated = componentPackageTaskRepository.updateByCondition(packageTaskDO, location);
            log.warn("kaniko build failed|componentPackageTaskId={}|task={}|updated={}|exception={}",
                    componentPackageTaskId, JSONObject.toJSONString(taskDO), updated, ExceptionUtils.getStackTrace(e));
            publisher.publishEvent(new FailedComponentPackageTaskEvent(this, componentPackageTaskId));
        }
    }

    private ComponentPackageTaskDO updateComponentTaskStatus(ComponentPackageTaskDO taskDO) {
        String oldStautus = taskDO.getTaskStatus();
        // 状态转移
        taskDO.setTaskStatus(STATE.toString());
        componentPackageTaskRepository.updateByCondition(taskDO, ComponentPackageTaskQueryCondition.builder().id(taskDO.getId()).build());
        log.info("actionName=RunningComponentPackageTask||status transitioned from {} to {}", oldStautus, STATE.toString());
        return componentPackageTaskRepository.getByCondition(ComponentPackageTaskQueryCondition.builder().id(taskDO.getId()).build());
    }
}
