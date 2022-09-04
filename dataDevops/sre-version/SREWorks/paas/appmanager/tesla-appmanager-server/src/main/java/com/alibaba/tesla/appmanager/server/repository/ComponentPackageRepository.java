package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;

import java.util.List;

public interface ComponentPackageRepository {
    long countByCondition(ComponentPackageQueryCondition condition);

    int deleteByCondition(ComponentPackageQueryCondition condition);

    int deleteByPrimaryKey(Long id);

    int insert(ComponentPackageDO record);

    List<ComponentPackageDO> selectByCondition(ComponentPackageQueryCondition condition);

    ComponentPackageDO getByCondition(ComponentPackageQueryCondition condition);

    int updateByCondition(ComponentPackageDO record, ComponentPackageQueryCondition condition);

    int updateByPrimaryKeySelective(ComponentPackageDO record);
}
