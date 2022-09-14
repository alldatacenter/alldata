package com.alibaba.tesla.appmanager.meta.helm.service;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.meta.helm.repository.condition.HelmMetaQueryCondition;
import com.alibaba.tesla.appmanager.meta.helm.repository.domain.HelmMetaDO;

public interface HelmMetaService {
    /**
     * 根据条件过滤HELM组件信息列表
     *
     * @param condition 过滤条件
     * @return List
     */
    Pagination<HelmMetaDO> list(HelmMetaQueryCondition condition);

    /**
     * 根据主键 ID 获取HELM组件信息
     *
     * @param id HELM组件主键 ID
     * @return HelmMetaDO
     */
    HelmMetaDO get(Long id, String namespaceId, String stageId);

    HelmMetaDO getByHelmPackageId(String appId, String helmPackageId, String namespaceId, String stageId);

    /**
     * 创建指定的HELM组件信息
     *
     * @param record HELM组件记录
     */
    int create(HelmMetaDO record);

    /**
     * 更新指定的HELM组件信息
     *
     * @param record    HELM组件记录
     * @param condition 过滤条件
     */
    int update(HelmMetaDO record, HelmMetaQueryCondition condition);

    /**
     * 通过HELM组件 ID 删除组件元信息
     *
     * @param id          HELM组件主键 ID
     * @param namespaceId Namespace ID
     * @param stageId     Stage ID
     * @return 删除数量
     */
    int delete(Long id, String namespaceId, String stageId);
}
