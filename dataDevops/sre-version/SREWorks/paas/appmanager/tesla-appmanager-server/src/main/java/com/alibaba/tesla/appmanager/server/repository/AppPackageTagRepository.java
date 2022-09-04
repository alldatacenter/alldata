package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageTagQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTagDO;

import java.util.List;

public interface AppPackageTagRepository {
    int insert(AppPackageTagDO record);

    List<AppPackageTagDO> query(List<Long> appPackageIdList, String tag);

    List<AppPackageTagDO> query(List<Long> appPackageIdList);

    int deleteByCondition(AppPackageTagQueryCondition condition);
}