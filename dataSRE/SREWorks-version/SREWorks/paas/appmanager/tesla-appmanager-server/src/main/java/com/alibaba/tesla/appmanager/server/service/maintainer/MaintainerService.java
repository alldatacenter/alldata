package com.alibaba.tesla.appmanager.server.service.maintainer;

/**
 * 系统维护 Service
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface MaintainerService {

    /**
     * 升级 namespaceId / stageId (针对各 meta 表新增的 namespaceId / stageId 空字段进行初始化)
     *
     * @param namespaceId Namespace ID
     * @param stageId     Stage ID
     */
    void upgradeNamespaceStage(String namespaceId, String stageId);
}
