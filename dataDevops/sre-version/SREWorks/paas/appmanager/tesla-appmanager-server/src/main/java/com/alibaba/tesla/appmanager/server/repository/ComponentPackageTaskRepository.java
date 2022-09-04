package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;

import java.util.List;

public interface ComponentPackageTaskRepository {
    long countByCondition(ComponentPackageTaskQueryCondition condition);

    int deleteByCondition(ComponentPackageTaskQueryCondition condition);

    int insert(ComponentPackageTaskDO record);

    List<ComponentPackageTaskDO> selectByCondition(ComponentPackageTaskQueryCondition condition);

    ComponentPackageTaskDO getByCondition(ComponentPackageTaskQueryCondition condition);

    int updateByCondition(ComponentPackageTaskDO record, ComponentPackageTaskQueryCondition condition);
}