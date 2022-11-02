package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.RtAppInstanceHistoryQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceHistoryDO;

import java.util.List;

public interface RtAppInstanceHistoryRepository {
    long countByCondition(RtAppInstanceHistoryQueryCondition condition);

    int deleteByCondition(RtAppInstanceHistoryQueryCondition condition);

    int insert(RtAppInstanceHistoryDO record);

    List<RtAppInstanceHistoryDO> selectByCondition(RtAppInstanceHistoryQueryCondition condition);

    RtAppInstanceHistoryDO getLatestByCondition(RtAppInstanceHistoryQueryCondition condition);

    int updateByCondition(RtAppInstanceHistoryDO record, RtAppInstanceHistoryQueryCondition condition);

    int deleteExpiredRecords(String appInstanceId, int instanceKeepDays);
}