package com.alibaba.tesla.appmanager.server.addon;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.appmanager.server.addon.req.ApplyAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.addon.req.CheckAddonInstanceExpiredReq;
import com.alibaba.tesla.appmanager.server.addon.req.ReleaseAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.addon.res.ApplyAddonInstanceRes;
import com.alibaba.tesla.appmanager.server.addon.task.AddonInstanceTaskService;
import com.alibaba.tesla.appmanager.server.repository.AddonInstanceRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AddonInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceTaskDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * Addon Instance 管理器
 *
 * @author qiuqiang.qq@alibaba-inc.com / yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component
public class AddonInstanceManagerImpl implements AddonInstanceManager {

    @Resource
    private AddonManager addonManager;

    @Resource
    private AddonInstanceRepository addonInstanceRepository;

    @Resource
    private AddonInstanceTaskService addonInstanceTaskService;

    /**
     * 根据指定条件查询唯一的 addon instance
     *
     * @param condition 查询条件
     * @return addon instance 对象
     */
    @Override
    public AddonInstanceDO getByCondition(AddonInstanceQueryCondition condition) {
        return addonInstanceRepository.getByCondition(condition);
    }

    /**
     * 资源申请
     *
     * @param request 资源申请请求
     */
    @Override
    public ApplyAddonInstanceRes applyAddonInstance(ApplyAddonInstanceReq request) {
        String namespaceId = request.getNamespaceId();
        String addonId = request.getAddonId();
        String addonName = request.getAddonName();
        Map<String, String> addonAttrs = request.getAddonAttrs();

        // 检查 Addon Instance 是否存在，如果存在直接返回实例 ID
        Addon addon = addonManager.getAddon(ComponentTypeEnum.RESOURCE_ADDON, request.getAddonId());
        AddonInstanceQueryCondition condition = AddonInstanceQueryCondition.builder()
                .namespaceId(namespaceId)
                .addonId(addonId)
                .addonName(addonName)
                .addonAttrs(addonAttrs)
                .build();
        AddonInstanceDO addonInstance = addonInstanceRepository.getByCondition(condition);
        if (addonInstance != null && StringUtils.isNotEmpty(addonInstance.getSignature())) {
            ComponentSchema lastSchema = SchemaUtil.toSchema(ComponentSchema.class, addonInstance.getAddonExt());
            if (!addon.checkExpired(CheckAddonInstanceExpiredReq.builder()
                    .namespaceId(namespaceId)
                    .addonId(addonId)
                    .addonName(addonName)
                    .addonAttrs(addonAttrs)
                    .lastSchema(lastSchema)
                    .lastSignature(addonInstance.getSignature())
                    .currentSchema(request.getSchema())
                    .build())) {
                log.info("after checking, the addon instance has not expired and can be reused|namespaceId={}|" +
                        "addonId={}|addonName={}|addonAttrs={}|lastSignature={}|addonInstanceId={}", namespaceId,
                        addonId, addonName, JSONObject.toJSONString(addonAttrs), addonInstance.getSignature(),
                        addonInstance.getAddonInstanceId());
                return ApplyAddonInstanceRes.builder()
                        .ready(true)
                        .addonInstanceId(addonInstance.getAddonInstanceId())
                        .build();
            } else {
                request.setLastSchema(lastSchema);
                log.info("after checking, the addon instance has expired and is ready to apply for a new instance|" +
                                "namespaceId={}|addonId={}|addonName={}|addonAttrs={}|lastSignature={}", namespaceId,
                        addonId, addonName, JSONObject.toJSONString(addonAttrs), addonInstance.getSignature());
            }
        }

        // 向系统申请一个新的 Addon Instance 的资源，并返回任务 ID
        AddonInstanceTaskDO task = addonInstanceTaskService.apply(request);
        return ApplyAddonInstanceRes.builder()
                .ready(false)
                .addonInstanceTaskId(task.getId())
                .build();
    }

    /**
     * 释放资源
     *
     * @param request 释放资源请求
     */
    @Override
    public String releaseAddonInstance(ReleaseAddonInstanceReq request) {
        String addonInstanceId = request.getAddonInstanceId();
        AddonInstanceQueryCondition condition = AddonInstanceQueryCondition.builder()
                .addonInstanceId(addonInstanceId)
                .build();
        AddonInstanceDO addonInstance = addonInstanceRepository.getByCondition(condition);
        if (addonInstance == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find addon instance %s", addonInstanceId));
        }

        // TODO: release 的 addon instance task 类型，要 return 回去
        Addon addon = addonManager.getAddon(ComponentTypeEnum.RESOURCE_ADDON, addonInstance.getAddonId());
        addon.release(request);
        return null;
    }
}
