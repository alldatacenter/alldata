package com.alibaba.tesla.appmanager.server.addon.task.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.AddonInstanceTaskVariableKey;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.enums.AddonInstanceTaskEventEnum;
import com.alibaba.tesla.appmanager.common.enums.AddonInstanceTaskStatusEnum;
import com.alibaba.tesla.appmanager.common.enums.DagTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.ObjectConvertUtil;
import com.alibaba.tesla.appmanager.server.addon.req.ApplyAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.addon.task.AddonInstanceTaskService;
import com.alibaba.tesla.appmanager.server.addon.task.dag.AddonInstanceTaskDag;
import com.alibaba.tesla.appmanager.server.addon.task.event.AddonInstanceTaskEvent;
import com.alibaba.tesla.appmanager.server.repository.AddonInstanceTaskRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonInstanceTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceTaskDO;
import com.alibaba.tesla.dag.services.DagInstService;
import com.google.common.base.Enums;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Addon Instance 任务服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class AddonInstanceTaskServiceImpl implements AddonInstanceTaskService {

    @Autowired
    private DagInstService dagInstService;

    @Autowired
    private AddonInstanceTaskRepository addonInstanceTaskRepository;

    @Autowired
    private ApplicationEventPublisher publisher;

    /**
     * 查询指定 addonInstanceTaskId 对应的任务运行对象
     *
     * @param addonInstanceTaskId Addon Instance 任务 ID
     * @return AddonInstanceTaskDO
     */
    @Override
    public AddonInstanceTaskDO query(long addonInstanceTaskId) {
        AddonInstanceTaskDO task = addonInstanceTaskRepository.selectByPrimaryKey(addonInstanceTaskId);
        if (task != null) {
            AddonInstanceTaskStatusEnum status = Enums
                    .getIfPresent(AddonInstanceTaskStatusEnum.class, task.getTaskStatus()).orNull();
            assert status != null;
            if (!status.isEnd()) {
                publisher.publishEvent(
                        new AddonInstanceTaskEvent(this, task.getId(), AddonInstanceTaskEventEnum.TRIGGER_UPDATE)
                );
            }
        }
        return task;
    }

    /**
     * 申请 Addon Instance，返回1一个 addon instance 申请任务 ID
     *
     * @param request 申请请求
     * @return addonInstanceTaskId
     */
    @Override
    public AddonInstanceTaskDO apply(ApplyAddonInstanceReq request) {
        String namespaceId = request.getNamespaceId();
        String addonId = request.getAddonId();
        String addonName = request.getAddonName();
        Map<String, String> addonAttrs = request.getAddonAttrs();
        String logSuffix = String.format("namespaceId=%s|addonId=%s|addonName=%s|addonAttrs=%s",
                namespaceId, addonId, addonName, JSONObject.toJSONString(addonAttrs));

        // 查询当前系统中是否存在正在运行中的 addon instance 申请任务，如果存在，直接返回原纪录
        // 注意：处于 WAIT_FOR_OP 等待人工处理的也直接返回
        AddonInstanceTaskQueryCondition condition = AddonInstanceTaskQueryCondition.builder()
                .namespaceId(namespaceId)
                .addonId(addonId)
                .addonName(addonName)
                .addonAttrs(addonAttrs)
                .taskStatusList(AddonInstanceTaskStatusEnum.runningStatusList())
                .build();
        List<AddonInstanceTaskDO> tasks = addonInstanceTaskRepository.selectByCondition(condition);
        if (tasks.size() > 1) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR,
                    String.format("multiple running addon instance tasks found, abort|%s", logSuffix));
        } else if (tasks.size() > 0) {
            return tasks.get(0);
        }

        // 插入数据库中记录
        AddonInstanceTaskDO task = AddonInstanceTaskDO.builder()
                .namespaceId(namespaceId)
                .addonId(addonId)
                .addonName(addonName)
                .addonVersion(DefaultConstant.AUTO_VERSION)
                .addonAttrs(ObjectConvertUtil.toJsonString(addonAttrs))
                .taskStatus(AddonInstanceTaskStatusEnum.PENDING.toString())
                .taskProcessId(0L)
                .taskExt(JSONObject.toJSONString(request.getSchema()))
                .build();
        addonInstanceTaskRepository.insert(task);
        log.info("addon instance task has inserted into database|addonInstanceTaskId={}|{}", task.getId(), logSuffix);

        // 启动一个新的申请任务并落库
        Long processId;
        JSONObject variables = new JSONObject();
        variables.put(DefaultConstant.DAG_TYPE, DagTypeEnum.APPLY_ADDON_INSTANCE_TASK);
        variables.put(AddonInstanceTaskVariableKey.APPLY_REQUEST, JSONObject.toJSONString(request));
        try {
            processId = dagInstService.start(AddonInstanceTaskDag.name, variables, true);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.DEPLOY_ERROR,
                    String.format("start addon instance task failed|%s|exception=%s",
                            logSuffix, ExceptionUtils.getStackTrace(e)));
        }
        log.info("addon instance task started|dagInstId={}|{}", processId, logSuffix);

        // 更新 addon instance task 的 task process id
        AddonInstanceTaskDO updateTask = AddonInstanceTaskDO.builder()
                .id(task.getId())
                .taskProcessId(processId)
                .build();
        addonInstanceTaskRepository.updateByPrimaryKey(updateTask);
        log.info("addon instance task has update process id in database|addonInstanceTaskId={}|dagInstId={}|{}",
                task.getId(), processId, logSuffix);
        return task;
    }
}
