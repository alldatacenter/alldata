package com.alibaba.tesla.appmanager.server.addon.task.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.DeployAppProvider;
import com.alibaba.tesla.appmanager.common.constants.CheckNullObject;
import com.alibaba.tesla.appmanager.common.enums.AddonInstanceTaskStatusEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.ObjectConvertUtil;
import com.alibaba.tesla.appmanager.common.util.ObjectUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.req.deploy.DeployAppLaunchReq;
import com.alibaba.tesla.appmanager.domain.res.deploy.DeployAppPackageLaunchRes;
import com.alibaba.tesla.appmanager.domain.schema.CustomAddonWorkloadSpec;
import com.alibaba.tesla.appmanager.server.addon.req.ApplyCustomAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.addon.task.CustomAddonInstanceTaskService;
import com.alibaba.tesla.appmanager.server.repository.CustomAddonInstanceTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonInstanceTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceTaskDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author yangjie.dyj@alibaba-inc.com
 * @ClassName:CustomAddonInstanceTaskServiceImpl
 * @DATE: 2020-11-26
 * @Description:
 **/
@Slf4j
@Service
public class CustomAddonInstanceTaskServiceImpl implements CustomAddonInstanceTaskService {
    @Autowired
    private CustomAddonInstanceTaskRepository customAddonInstanceTaskRepository;

    @Autowired
    private DeployAppProvider deployAppProvider;

    /**
     * 查询 custom addon instance 任务
     *
     * @param customAddonInstanceTaskId
     * @return
     */
    @Override
    public CustomAddonInstanceTaskDO query(long customAddonInstanceTaskId) {
        CustomAddonInstanceTaskDO customAddonInstanceTaskDO = customAddonInstanceTaskRepository.selectByPrimaryKey(
                customAddonInstanceTaskId);
        ObjectUtil.checkNull(CheckNullObject.builder()
                .actionName("query")
                .objectName("customAddonInstanceTaskDO")
                .checkObject(customAddonInstanceTaskDO)
                .build());
        return customAddonInstanceTaskDO;
    }

    /**
     * 申请 custom addon instance 任务
     *
     * @param request
     * @return
     */
    @Override
    public CustomAddonInstanceTaskDO apply(ApplyCustomAddonInstanceReq request) {
        String namespaceId = request.getNamespaceId();
        String addonId = request.getAddonId();
        String addonVersion = request.getAddonVersion();
        String addonName = request.getAddonName();
        Map<String, String> addonAttrs = request.getAddonAttrs();
        String logSuffix = String.format("namespaceId=%s|addonId=%s|addonVersion=%s|addonName=%s|addonAttrs=%s",
                namespaceId, addonId, addonVersion, addonName, JSONObject.toJSONString(addonAttrs));

        // 查询当前系统中是否存在正在运行中的 custom addon instance 申请任务，如果存在，直接返回原纪录
        // 注意：处于 WAIT_FOR_OP 等待人工处理的也直接返回
        AddonInstanceTaskQueryCondition condition = AddonInstanceTaskQueryCondition.builder()
                .namespaceId(namespaceId)
                .addonId(addonId)
                .addonVersion(addonVersion)
                .addonName(addonName)
                .addonAttrs(addonAttrs)
                .taskStatusList(AddonInstanceTaskStatusEnum.runningStatusList())
                .build();
        List<CustomAddonInstanceTaskDO> tasks = customAddonInstanceTaskRepository.selectByCondition(condition);
        if (tasks.size() > 1) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                    String.format("multiple running addon instance tasks found, abort|%s", logSuffix));
        } else if (tasks.size() > 0) {
            return tasks.get(0);
        }

        // 插入数据库中记录
        CustomAddonInstanceTaskDO task = CustomAddonInstanceTaskDO.builder()
                .namespaceId(namespaceId)
                .addonId(addonId)
                .addonName(addonName)
                .addonVersion(addonVersion)
                .addonAttrs(ObjectConvertUtil.toJsonString(addonAttrs))
                .taskStatus(AddonInstanceTaskStatusEnum.PENDING.toString())
                .deployAppId(0L)
                .taskExt(SchemaUtil.toYamlMapStr(request.getCustomAddonSchema()))
                .build();
        customAddonInstanceTaskRepository.insert(task);
        log.info("custom addon instance task has inserted into database|addonInstanceTaskId={}|{}", task.getId(), logSuffix);

        //启动一个新的custom addon申请任务并落库
        DeployAppPackageLaunchRes deployAppPackageLaunchRes = deployAppProvider.launch(getLaunchDeployReq(request), request.getCreator());
        CustomAddonInstanceTaskDO updateTask = CustomAddonInstanceTaskDO.builder()
                .id(task.getId())
                .deployAppId(deployAppPackageLaunchRes.getDeployAppId())
                .taskStatus(AddonInstanceTaskStatusEnum.RUNNING.toString())
                .build();
        customAddonInstanceTaskRepository.updateByPrimaryKey(updateTask);
        return task;
    }

    private DeployAppLaunchReq getLaunchDeployReq(ApplyCustomAddonInstanceReq request) {
        CustomAddonWorkloadSpec customAddonWorkloadSpec = request.getCustomAddonSchema().getSpec().getWorkload().getSpec();

        String appConfig = SchemaUtil.toYamlMapStr(customAddonWorkloadSpec.getApplicationConfig());
        return DeployAppLaunchReq.builder()
                .configuration(appConfig)
                .autoEnvironment(Boolean.TRUE.toString())
                .build();
    }
}
