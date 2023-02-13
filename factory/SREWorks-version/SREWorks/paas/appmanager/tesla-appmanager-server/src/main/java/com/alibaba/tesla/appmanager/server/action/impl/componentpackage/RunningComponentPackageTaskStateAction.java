package com.alibaba.tesla.appmanager.server.action.impl.componentpackage;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskStateEnum;
import com.alibaba.tesla.appmanager.domain.req.componentpackage.BuildComponentHandlerReq;
import com.alibaba.tesla.appmanager.domain.res.componentpackage.LaunchBuildComponentHandlerRes;
import com.alibaba.tesla.appmanager.server.action.ComponentPackageTaskStateAction;
import com.alibaba.tesla.appmanager.server.event.componentpackage.FailedComponentPackageTaskEvent;
import com.alibaba.tesla.appmanager.server.event.componentpackage.SucceedComponentPackageTaskEvent;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

/**
 * Component Package State 处理 Action - RUNNING
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service("RunningComponentPackageTaskStateAction")
public class RunningComponentPackageTaskStateAction implements ComponentPackageTaskStateAction, ApplicationRunner {

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
        if (!systemProperties.isEnableKaniko()) {
            publisher.publishEvent(new ComponentPackageTaskStateActionLoadedEvent(
                    this, STATE.toString(), this.getClass().getSimpleName()));
        }
    }

    /**
     * 自身逻辑处理
     *
     * @param packageTask 部署工单
     */
    @Override
    public void run(ComponentPackageTaskDO packageTask) {
        updateComponentTaskStatus(packageTask);
        ComponentPackageTaskDO taskDO = componentPackageTaskRepository
                .getByCondition(ComponentPackageTaskQueryCondition.builder()
                        .id(packageTask.getId())
                        .build());
        Long componentPackageTaskId = taskDO.getId();
        String appId = taskDO.getAppId();
        String componentType = taskDO.getComponentType();
        String componentName = taskDO.getComponentName();
        String packageVersion = taskDO.getPackageVersion();
        String packageCreator = taskDO.getPackageCreator();
        String packageOptions = taskDO.getPackageOptions();
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
            taskDO.setTaskLog(message);
            componentPackageTaskRepository.updateByCondition(taskDO, ComponentPackageTaskQueryCondition.builder()
                    .id(taskDO.getId())
                    .build());
            publisher.publishEvent(new FailedComponentPackageTaskEvent(this, componentPackageTaskId));
            return;
        }

        // 生成导出配置文件
        LaunchBuildComponentHandlerRes componentPackageInfo;
        try {
            componentPackageInfo = componentPackageBuilderService.build(BuildComponentHandlerReq.builder()
                    .appId(taskDO.getAppId())
                    .namespaceId(taskDO.getNamespaceId())
                    .stageId(taskDO.getStageId())
                    .componentType(taskDO.getComponentType())
                    .componentName(taskDO.getComponentName())
                    .version(taskDO.getPackageVersion())
                    .options(JSONObject.parseObject(taskDO.getPackageOptions()))
                    .build());
        } catch (IOException e) {
            log.warn("cannot create temp file|componentPackageTaskId={}|exception={}",
                    componentPackageTaskId, ExceptionUtils.getStackTrace(e));
            taskDO.setTaskLog(e.getMessage());
            componentPackageTaskRepository.updateByCondition(taskDO, ComponentPackageTaskQueryCondition.builder()
                    .id(taskDO.getId())
                    .build());
            return;
        }

        // 增加 Component Package 包记录
        ComponentPackageDO componentPackageDO = ComponentPackageDO.builder()
                .appId(appId)
                .componentType(componentType)
                .componentName(componentName)
                .packageVersion(packageVersion)
                .packageCreator(packageCreator)
                .packageMd5(componentPackageInfo.getPackageMd5())
                .packagePath(componentPackageInfo.getStorageFile().toPath())
                .packageOptions(packageOptions)
                .componentSchema(componentPackageInfo.getPackageMetaYaml())
                .build();
        taskDO.setPackagePath(componentPackageInfo.getStorageFile().toPath());
        taskDO.setPackageMd5(componentPackageInfo.getPackageMd5());
        taskDO.setTaskLog(componentPackageInfo.getLogContent());
        updateDatabaseRecord(componentPackageDO, taskDO);
        log.info("component package task has inserted to db|componentPackageTaskId={}|" +
                        "componentPackageId={}|appId={}|componentType={}|componentName={}|version={}|md5={}",
                componentPackageTaskId, componentPackageDO.getId(), appId, componentType,
                componentName, packageVersion, componentPackageInfo.getPackageMd5());
        publisher.publishEvent(new SucceedComponentPackageTaskEvent(this, componentPackageTaskId));
    }

    /**
     * 事务内更新 Database 记录
     *
     * @param componentPackageDO Component Package 记录
     * @param taskDO             Component Package Task 记录
     */
    @Transactional
    public void updateDatabaseRecord(ComponentPackageDO componentPackageDO, ComponentPackageTaskDO taskDO) {
        componentPackageRepository.insert(componentPackageDO);
        taskDO.setComponentPackageId(componentPackageDO.getId());
        componentPackageTaskRepository.updateByCondition(taskDO, ComponentPackageTaskQueryCondition.builder()
                .id(taskDO.getId())
                .build());
    }

    private void updateComponentTaskStatus(ComponentPackageTaskDO taskDO) {
        String oldStautus = taskDO.getTaskStatus();
        // 状态转移
        taskDO.setTaskStatus(STATE.toString());
        componentPackageTaskRepository.updateByCondition(taskDO, ComponentPackageTaskQueryCondition.builder().id(taskDO.getId()).build());
        log.info("actionName=RunningComponentPackageTask||status transitioned from {} to {}", oldStautus, STATE.toString());
    }
}
