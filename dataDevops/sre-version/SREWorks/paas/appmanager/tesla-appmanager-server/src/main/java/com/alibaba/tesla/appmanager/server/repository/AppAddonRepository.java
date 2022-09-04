package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.AppAddonQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppAddonDO;

import java.util.List;

public interface AppAddonRepository {

    long countByCondition(AppAddonQueryCondition condition);

    int deleteByCondition(AppAddonQueryCondition condition);

    int insert(AppAddonDO record);

    List<AppAddonDO> selectByCondition(AppAddonQueryCondition condition);

    int updateByCondition(AppAddonDO record, AppAddonQueryCondition condition);
}