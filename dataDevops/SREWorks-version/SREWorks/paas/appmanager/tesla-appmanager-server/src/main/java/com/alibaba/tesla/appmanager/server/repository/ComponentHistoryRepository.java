package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.ComponentHistoryQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentHistoryDO;

import java.util.List;

public interface ComponentHistoryRepository {

    long countByCondition(ComponentHistoryQueryCondition condition);

    int deleteByCondition(ComponentHistoryQueryCondition condition);

    int insert(ComponentHistoryDO record);

    List<ComponentHistoryDO> selectByConditionWithBLOBs(ComponentHistoryQueryCondition condition);

    List<ComponentHistoryDO> selectByCondition(ComponentHistoryQueryCondition condition);

    int updateByConditionWithBLOBs(ComponentHistoryDO record, ComponentHistoryQueryCondition condition);

    int updateByCondition(ComponentHistoryDO record, ComponentHistoryQueryCondition condition);
}