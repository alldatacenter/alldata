package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.DeployAppQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;

import java.util.List;

public interface DeployAppRepository {
    long countByCondition(DeployAppQueryCondition condition);

    int deleteByCondition(DeployAppQueryCondition condition);

    int deleteByPrimaryKey(Long id);

    int insert(DeployAppDO record);

    List<DeployAppDO> selectByCondition(DeployAppQueryCondition condition);

    DeployAppDO selectByPrimaryKey(Long id);

    int updateByConditionSelective(DeployAppDO record, DeployAppQueryCondition condition);

    int updateByPrimaryKey(DeployAppDO record);
}