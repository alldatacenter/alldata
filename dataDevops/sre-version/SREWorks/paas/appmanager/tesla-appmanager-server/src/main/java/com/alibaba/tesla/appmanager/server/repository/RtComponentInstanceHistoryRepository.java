package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.RtComponentInstanceHistoryQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceHistoryDO;

import java.util.List;

public interface RtComponentInstanceHistoryRepository {
    long countByCondition(RtComponentInstanceHistoryQueryCondition condition);

    int deleteByCondition(RtComponentInstanceHistoryQueryCondition condition);

    int insert(RtComponentInstanceHistoryDO record);

    List<RtComponentInstanceHistoryDO> selectByCondition(RtComponentInstanceHistoryQueryCondition condition);

    RtComponentInstanceHistoryDO getLatestByCondition(RtComponentInstanceHistoryQueryCondition condition);

    int updateByCondition(RtComponentInstanceHistoryDO record, RtComponentInstanceHistoryQueryCondition condition);

    int deleteExpiredRecords(String componentInstanceId, int instanceKeepDays);
}