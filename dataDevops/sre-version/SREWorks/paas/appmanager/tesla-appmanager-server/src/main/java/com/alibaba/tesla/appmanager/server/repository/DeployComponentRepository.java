package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.DeployComponentQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDO;

import java.util.List;

public interface DeployComponentRepository {
    long countByCondition(DeployComponentQueryCondition condition);

    int deleteByCondition(DeployComponentQueryCondition condition);

    int deleteByPrimaryKey(Long id);

    int insert(DeployComponentDO record);

    List<DeployComponentDO> selectByCondition(DeployComponentQueryCondition condition);

    DeployComponentDO selectByPrimaryKey(Long id);

    int updateByConditionSelective(DeployComponentDO record, DeployComponentQueryCondition condition);

    int updateByPrimaryKey(DeployComponentDO record);
}