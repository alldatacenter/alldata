package com.alibaba.tesla.appmanager.meta.k8smicroservice.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.K8sMicroServiceMetaRepository;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.condition.K8sMicroserviceMetaQueryCondition;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain.K8sMicroServiceMetaDO;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.service.K8sMicroserviceMetaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

/**
 * K8S 微应用元信息服务
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Service
@Slf4j
public class K8SMicroserviceMetaServiceImpl implements K8sMicroserviceMetaService {

    @Autowired
    private K8sMicroServiceMetaRepository k8sMicroServiceMetaRepository;

    /**
     * 根据条件过滤微应用元信息列表
     *
     * @param condition 过滤条件
     * @return List
     */
    @Override
    public Pagination<K8sMicroServiceMetaDO> list(K8sMicroserviceMetaQueryCondition condition) {
        List<K8sMicroServiceMetaDO> metaList = k8sMicroServiceMetaRepository.selectByCondition(condition);
        return Pagination.valueOf(metaList, Function.identity());
    }

    /**
     * 根据条件获取单个微应用元信息
     *
     * @param condition 过滤条件
     * @return K8sMicroServiceMetaDO
     */
    @Override
    public K8sMicroServiceMetaDO get(K8sMicroserviceMetaQueryCondition condition) {
        List<K8sMicroServiceMetaDO> metaList = k8sMicroServiceMetaRepository.selectByCondition(condition);
        if (metaList.size() == 0) {
            return null;
        } else if (metaList.size() == 1) {
            return metaList.get(0);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple k8s microservice meta found, abort|condition=%s",
                            JSONObject.toJSONString(condition)));
        }
    }

    /**
     * 根据主键 ID 获取微应用元信息
     *
     * @param id 微应用元信息主键 ID
     * @return K8sMicroServiceMetaDO
     */
    @Override
    public K8sMicroServiceMetaDO get(Long id) {
        return k8sMicroServiceMetaRepository.selectByPrimaryKey(id);
    }

    /**
     * 根据 appId + microServiceId + namespaceId + stageId 获取微应用元信息
     *
     * @param appId          应用 ID
     * @param microServiceId 微服务标识
     * @param arch           架构
     * @param namespaceId    Namespace ID
     * @param stageId        Stage ID
     * @return K8sMicroServiceMetaDO
     */
    @Override
    public K8sMicroServiceMetaDO getByMicroServiceId(
            String appId, String microServiceId, String arch, String namespaceId, String stageId) {
        K8sMicroserviceMetaQueryCondition condition = K8sMicroserviceMetaQueryCondition.builder()
                .microServiceId(microServiceId)
                .appId(appId)
                .namespaceId(namespaceId)
                .stageId(stageId)
                .arch(arch)
                .withBlobs(true)
                .build();
        List<K8sMicroServiceMetaDO> metaList = k8sMicroServiceMetaRepository.selectByCondition(condition);
        if (metaList.size() > 0) {
            return metaList.get(0);
        } else {
            return null;
        }
    }

    /**
     * 更新指定的微应用元信息
     *
     * @param record 微应用元信息记录
     */
    @Override
    public int update(K8sMicroServiceMetaDO record, K8sMicroserviceMetaQueryCondition condition) {
        return k8sMicroServiceMetaRepository.updateByCondition(record, condition);
    }

    /**
     * 创建指定的微应用元信息
     *
     * @param record 微应用元信息记录
     */
    @Override
    public int create(K8sMicroServiceMetaDO record) {
        return k8sMicroServiceMetaRepository.insert(record);
    }

    /**
     * 根据主键 ID 删除微应用元信息
     *
     * @param id 微应用元信息主键 ID
     */
    @Override
    public int delete(Long id) {
        return k8sMicroServiceMetaRepository.deleteByPrimaryKey(id);
    }

    /**
     * 根据条件删除微应用元信息
     *
     * @param condition 查询条件
     */
    @Override
    public int delete(K8sMicroserviceMetaQueryCondition condition) {
        return k8sMicroServiceMetaRepository.deleteByCondition(condition);
    }
}
