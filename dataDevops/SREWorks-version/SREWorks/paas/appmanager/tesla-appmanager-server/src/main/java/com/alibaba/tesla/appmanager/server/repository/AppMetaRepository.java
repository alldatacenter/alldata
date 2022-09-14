package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.AppMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppMetaDO;

import java.util.List;

public interface AppMetaRepository {
    long countByCondition(AppMetaQueryCondition condition);

    int deleteByCondition(AppMetaQueryCondition condition);

    int insert(AppMetaDO record);

    List<AppMetaDO> selectByCondition(AppMetaQueryCondition condition);

    int updateByCondition(AppMetaDO record, AppMetaQueryCondition condition);
}