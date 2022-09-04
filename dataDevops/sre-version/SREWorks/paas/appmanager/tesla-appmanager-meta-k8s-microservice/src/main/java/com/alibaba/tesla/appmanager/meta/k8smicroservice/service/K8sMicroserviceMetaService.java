package com.alibaba.tesla.appmanager.meta.k8smicroservice.service;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.condition.K8sMicroserviceMetaQueryCondition;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain.K8sMicroServiceMetaDO;

/**
 * K8S 微应用元信息服务
 *
 * @author qianmo.zm@alibaba-inc.com
 */
public interface K8sMicroserviceMetaService {

    /**
     * 根据条件过滤微应用元信息列表
     *
     * @param condition 过滤条件
     * @return List
     */
    Pagination<K8sMicroServiceMetaDO> list(K8sMicroserviceMetaQueryCondition condition);

    /**
     * 根据条件获取单个微应用元信息
     *
     * @param condition 过滤条件
     * @return K8sMicroServiceMetaDO
     */
    K8sMicroServiceMetaDO get(K8sMicroserviceMetaQueryCondition condition);

    /**
     * 根据主键 ID 获取微应用元信息
     *
     * @param id 微应用元信息主键 ID
     * @return K8sMicroServiceMetaDO
     */
    K8sMicroServiceMetaDO get(Long id);

    /**
     * 根据 appId + microServiceId + namespaceId + stageId 获取微应用元信息
     *
     * @param appId 应用 ID
     * @param microServiceId 微服务标识
     * @param arch 架构
     * @param namespaceId Namespace ID
     * @param stageId Stage ID
     * @return K8sMicroServiceMetaDO
     */
    K8sMicroServiceMetaDO getByMicroServiceId(
            String appId, String microServiceId, String arch, String namespaceId, String stageId);

    /**
     * 更新指定的微应用元信息
     *
     * @param record 微应用元信息记录
     */
    int update(K8sMicroServiceMetaDO record, K8sMicroserviceMetaQueryCondition condition);

    /**
     * 创建指定的微应用元信息
     *
     * @param record 微应用元信息记录
     */
    int create(K8sMicroServiceMetaDO record);

    /**
     * 根据主键 ID 删除微应用元信息
     *
     * @param id 微应用元信息主键 ID
     */
    int delete(Long id);

    /**
     * 根据条件删除应用组件
     *
     * @param condition 查询条件
     */
    int delete(K8sMicroserviceMetaQueryCondition condition);
}
