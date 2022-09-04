package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageComponentRelQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageComponentRelDO;

import java.util.List;

public interface AppPackageComponentRelRepository {
    long countByCondition(AppPackageComponentRelQueryCondition condition);

    int deleteByCondition(AppPackageComponentRelQueryCondition condition);

    int insert(AppPackageComponentRelDO record);

    List<AppPackageComponentRelDO> selectByCondition(AppPackageComponentRelQueryCondition condition);

    int updateByCondition(AppPackageComponentRelDO record, AppPackageComponentRelQueryCondition condition);
}
