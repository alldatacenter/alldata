package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.tesla.appmanager.api.provider.MaintainerProvider;
import com.alibaba.tesla.appmanager.server.service.maintainer.MaintainerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 系统维护 Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class MaintainerProviderImpl implements MaintainerProvider {

    @Autowired
    private MaintainerService maintainerService;

    /**
     * 升级 namespaceId / stageId (针对各 meta 表新增的 namespaceId / stageId 空字段进行初始化)
     *
     * @param namespaceId Namespace ID
     * @param stageId     Stage ID
     */
    @Override
    public void upgradeNamespaceStage(String namespaceId, String stageId) {
        maintainerService.upgradeNamespaceStage(namespaceId, stageId);
    }
}
