package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.DeployAppAttrQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppAttrDO;

import java.util.List;

public interface DeployAppAttrRepository {
    int deleteByCondition(DeployAppAttrQueryCondition condition);

    int insert(DeployAppAttrDO record);

    List<DeployAppAttrDO> selectByCondition(DeployAppAttrQueryCondition condition);

    int updateByPrimaryKey(DeployAppAttrDO record);
}