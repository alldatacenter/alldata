package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.ClusterQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ClusterDO;

import java.util.List;

public interface ClusterRepository {

    long countByCondition(ClusterQueryCondition condition);

    int deleteByCondition(ClusterQueryCondition condition);

    int insert(ClusterDO record);

    List<ClusterDO> selectByCondition(ClusterQueryCondition condition);

    int updateByCondition(ClusterDO record, ClusterQueryCondition condition);
}