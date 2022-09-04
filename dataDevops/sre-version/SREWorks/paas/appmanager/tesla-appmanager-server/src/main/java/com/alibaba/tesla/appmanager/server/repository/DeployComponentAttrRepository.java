package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.DeployComponentAttrQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentAttrDO;

import java.util.List;

public interface DeployComponentAttrRepository {

    int deleteByCondition(DeployComponentAttrQueryCondition condition);

    int insert(DeployComponentAttrDO record);

    List<DeployComponentAttrDO> selectByCondition(DeployComponentAttrQueryCondition condition);

    int updateByPrimaryKey(DeployComponentAttrDO record);
}