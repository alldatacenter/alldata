package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.AddonInstanceTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AddonInstanceTaskDO;

import java.util.List;

public interface AddonInstanceTaskRepository {
    long countByCondition(AddonInstanceTaskQueryCondition condition);

    int deleteByCondition(AddonInstanceTaskQueryCondition condition);

    int deleteByPrimaryKey(Long id);

    int insert(AddonInstanceTaskDO record);

    int insertOrUpdate(AddonInstanceTaskDO record);

    List<AddonInstanceTaskDO> selectByCondition(AddonInstanceTaskQueryCondition condition);

    AddonInstanceTaskDO selectByPrimaryKey(Long id);

    int updateByCondition(AddonInstanceTaskDO record, AddonInstanceTaskQueryCondition condition);

    int updateByPrimaryKey(AddonInstanceTaskDO record);
}