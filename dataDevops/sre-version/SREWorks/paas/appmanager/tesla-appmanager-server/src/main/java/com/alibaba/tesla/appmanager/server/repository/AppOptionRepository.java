package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.AppOptionQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppOptionDO;

import java.util.List;

public interface AppOptionRepository {
    long countByCondition(AppOptionQueryCondition condition);

    int deleteByCondition(AppOptionQueryCondition condition);

    int insert(AppOptionDO record);

    int batchInsert(List<AppOptionDO> records);

    List<AppOptionDO> selectByCondition(AppOptionQueryCondition condition);

    AppOptionDO getByCondition(AppOptionQueryCondition condition);

    int updateByCondition(AppOptionDO record, AppOptionQueryCondition condition);
}