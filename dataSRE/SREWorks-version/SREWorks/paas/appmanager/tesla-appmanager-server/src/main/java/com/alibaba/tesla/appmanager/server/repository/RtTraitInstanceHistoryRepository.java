package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.RtTraitInstanceHistoryQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtTraitInstanceHistoryDO;

import java.util.List;

public interface RtTraitInstanceHistoryRepository {
    long countByCondition(RtTraitInstanceHistoryQueryCondition condition);

    int deleteByCondition(RtTraitInstanceHistoryQueryCondition condition);

    int insert(RtTraitInstanceHistoryDO record);

    List<RtTraitInstanceHistoryDO> selectByCondition(RtTraitInstanceHistoryQueryCondition condition);

    int updateByCondition(RtTraitInstanceHistoryDO record, RtTraitInstanceHistoryQueryCondition condition);
}