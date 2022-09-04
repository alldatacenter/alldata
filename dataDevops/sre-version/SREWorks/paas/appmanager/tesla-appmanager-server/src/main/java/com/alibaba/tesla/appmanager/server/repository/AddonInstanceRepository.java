package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.AddonInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceDO;

import java.util.List;

public interface AddonInstanceRepository {
    long countByCondition(AddonInstanceQueryCondition condition);

    int deleteByCondition(AddonInstanceQueryCondition condition);

    int insert(AddonInstanceDO record);

    AddonInstanceDO getByCondition(AddonInstanceQueryCondition condition);

    List<AddonInstanceDO> selectByCondition(AddonInstanceQueryCondition condition);

    int updateByPrimaryKey(AddonInstanceDO record);
}