package com.alibaba.tesla.appmanager.meta.k8smicroservice.repository;

import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.condition.K8sMicroserviceMetaQueryCondition;
import com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain.K8sMicroServiceMetaDO;

import java.util.List;

public interface K8sMicroServiceMetaRepository {
    long countByCondition(K8sMicroserviceMetaQueryCondition condition);

    int deleteByCondition(K8sMicroserviceMetaQueryCondition condition);

    int deleteByPrimaryKey(Long id);

    int insert(K8sMicroServiceMetaDO record);

    List<K8sMicroServiceMetaDO> selectByCondition(K8sMicroserviceMetaQueryCondition condition);

    K8sMicroServiceMetaDO selectByPrimaryKey(Long id);

    int updateByCondition(K8sMicroServiceMetaDO record, K8sMicroserviceMetaQueryCondition condition);
}