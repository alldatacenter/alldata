package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.ReleaseQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ReleaseDO;

import java.util.List;

public interface ReleaseRepository {
    long countByCondition(ReleaseQueryCondition condition);

    int deleteByCondition(ReleaseQueryCondition condition);

    int insert(ReleaseDO record);

    List<ReleaseDO> selectByCondition(ReleaseQueryCondition condition);

    ReleaseDO getByCondition(ReleaseQueryCondition condition);

    int updateByCondition(ReleaseDO record, ReleaseQueryCondition condition);
}