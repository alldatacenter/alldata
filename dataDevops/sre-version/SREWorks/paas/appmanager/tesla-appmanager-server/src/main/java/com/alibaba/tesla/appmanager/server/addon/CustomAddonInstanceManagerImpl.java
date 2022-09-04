package com.alibaba.tesla.appmanager.server.addon;

import com.alibaba.tesla.appmanager.common.constants.CheckNullObject;
import com.alibaba.tesla.appmanager.common.util.ObjectUtil;
import com.alibaba.tesla.appmanager.server.addon.req.ApplyCustomAddonInstanceReq;
import com.alibaba.tesla.appmanager.server.addon.res.ApplyCustomAddonInstanceRes;
import com.alibaba.tesla.appmanager.server.addon.task.CustomAddonInstanceTaskService;
import com.alibaba.tesla.appmanager.server.repository.CustomAddonInstanceRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.CustomAddonInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceDO;
import com.alibaba.tesla.appmanager.server.repository.domain.CustomAddonInstanceTaskDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author yangjie.dyj@alibaba-inc.com
 * @ClassName:CustomAddonInstanceManagerImpl
 * @DATE: 2020-11-26
 * @Description:
 **/
@Slf4j
@Component
public class CustomAddonInstanceManagerImpl implements CustomAddonInstanceManager {
    @Autowired
    CustomAddonInstanceRepository customAddonInstanceRepository;

    @Autowired
    CustomAddonInstanceTaskService customAddonInstanceTaskService;

    /**
     * 根据指定条件查询唯一的 custom addon instance
     *
     * @param condition
     * @return
     */
    @Override
    public CustomAddonInstanceDO getByCondition(CustomAddonInstanceQueryCondition condition) {
        CustomAddonInstanceDO customAddonInstanceDO = customAddonInstanceRepository.getByCondition(condition);
        ObjectUtil.checkNull(CheckNullObject.builder()
                .actionName("getByCondition")
                .checkObject(customAddonInstanceDO)
                .objectName("customAddonInstanceDO")
                .build());
        return customAddonInstanceDO;
    }

    /**
     * custom addon 资源申请
     *
     * @param request
     * @return
     */
    @Override
    public ApplyCustomAddonInstanceRes applyCustomAddonInstance(ApplyCustomAddonInstanceReq request) {
        String namespaceId = request.getNamespaceId();
        String addonId = request.getAddonId();
        String addonVersion = request.getAddonVersion();
        String addonName = request.getAddonName();
        Map<String, String> addonAttrs = request.getAddonAttrs();

        // 检查 Addon Instance 是否存在，如果存在直接返回实例 ID
        CustomAddonInstanceQueryCondition condition = CustomAddonInstanceQueryCondition.builder()
                .namespaceId(namespaceId)
                .addonId(addonId)
                .addonVersion(addonVersion)
                .addonName(addonName)
                .addonAttrs(addonAttrs)
                .build();
        CustomAddonInstanceDO addonInstance = customAddonInstanceRepository.getByCondition(condition);
        if (addonInstance != null) {
            return ApplyCustomAddonInstanceRes.builder()
                    .ready(true)
                    .customAddonInstanceId(addonInstance.getAddonInstanceId())
                    .build();
        }

        //新建custom addon部署单进行部署
        CustomAddonInstanceTaskDO task = customAddonInstanceTaskService.apply(request);
        ObjectUtil.checkNull(CheckNullObject.builder()
                .actionName("applyCustomAddonInstance")
                .checkObject(task)
                .objectName("task")
                .build());
        return ApplyCustomAddonInstanceRes.builder()
                .ready(false)
                .customAddonInstanceTaskId(task.getId())
                .build();
    }
}
