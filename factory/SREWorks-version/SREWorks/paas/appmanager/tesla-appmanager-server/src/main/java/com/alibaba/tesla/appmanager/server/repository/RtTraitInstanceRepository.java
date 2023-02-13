package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.RtTraitInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtTraitInstanceDO;

import java.util.List;

public interface RtTraitInstanceRepository {
    long countByCondition(RtTraitInstanceQueryCondition condition);

    int deleteByCondition(RtTraitInstanceQueryCondition condition);

    int insert(RtTraitInstanceDO record);

    List<RtTraitInstanceDO> selectByCondition(RtTraitInstanceQueryCondition condition);

    int updateByCondition(RtTraitInstanceDO record, RtTraitInstanceQueryCondition condition);
}