package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.NamespaceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.NamespaceDO;

import java.util.List;

public interface NamespaceRepository {
    long countByCondition(NamespaceQueryCondition condition);

    int deleteByCondition(NamespaceQueryCondition condition);

    int insert(NamespaceDO record);

    List<NamespaceDO> selectByCondition(NamespaceQueryCondition condition);

    int updateByCondition(NamespaceDO record, NamespaceQueryCondition condition);
}