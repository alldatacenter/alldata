package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTaskDO;

import java.util.List;

public interface AppPackageTaskRepository {
    long countByCondition(AppPackageTaskQueryCondition condition);

    int insert(AppPackageTaskDO record);

    List<AppPackageTaskDO> selectByCondition(AppPackageTaskQueryCondition condition);

    int updateByCondition(AppPackageTaskDO record, AppPackageTaskQueryCondition condition);

    int deleteByCondition(AppPackageTaskQueryCondition condition);
}