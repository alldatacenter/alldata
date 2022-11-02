package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.UnitQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.UnitDO;

import java.util.List;

public interface UnitRepository {
    long countByCondition(UnitQueryCondition condition);

    int deleteByCondition(UnitQueryCondition condition);

    int insert(UnitDO record);

    List<UnitDO> selectByCondition(UnitQueryCondition condition);

    int updateByCondition(UnitDO record, UnitQueryCondition condition);
}