package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.RtAppInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtAppInstanceDO;

import java.util.List;

public interface RtAppInstanceRepository {
    long countByCondition(RtAppInstanceQueryCondition condition);

    int deleteByCondition(RtAppInstanceQueryCondition condition);

    int insert(RtAppInstanceDO record);

    List<RtAppInstanceDO> selectByCondition(RtAppInstanceQueryCondition condition);

    RtAppInstanceDO getByCondition(RtAppInstanceQueryCondition condition);

    int updateByCondition(RtAppInstanceDO record, RtAppInstanceQueryCondition condition);
}