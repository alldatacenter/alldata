package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.domain.dto.AppPackageVersionCountDTO;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageVersionCountQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;

import java.util.List;

public interface AppPackageRepository {
    long countByCondition(AppPackageQueryCondition condition);

    int deleteByCondition(AppPackageQueryCondition condition);

    int deleteByPrimaryKey(Long id);

    int insert(AppPackageDO record);

    List<AppPackageDO> selectByCondition(AppPackageQueryCondition condition);

    AppPackageDO getByCondition(AppPackageQueryCondition condition);

    int updateByCondition(AppPackageDO record, AppPackageQueryCondition condition);

    int updateByPrimaryKeySelective(AppPackageDO record);

    List<AppPackageVersionCountDTO> countPackageByCondition(AppPackageVersionCountQueryCondition condition);
}
