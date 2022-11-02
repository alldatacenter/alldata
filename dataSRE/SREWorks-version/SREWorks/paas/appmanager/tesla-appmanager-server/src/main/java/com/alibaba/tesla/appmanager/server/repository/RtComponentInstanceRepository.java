package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.RtComponentInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceDO;

import java.util.List;

public interface RtComponentInstanceRepository {
    long countByCondition(RtComponentInstanceQueryCondition condition);

    int deleteByCondition(RtComponentInstanceQueryCondition condition);

    int insert(RtComponentInstanceDO record);

    List<RtComponentInstanceDO> selectByCondition(RtComponentInstanceQueryCondition condition);

    RtComponentInstanceDO getByCondition(RtComponentInstanceQueryCondition condition);

    int updateByCondition(RtComponentInstanceDO record, RtComponentInstanceQueryCondition condition);
}