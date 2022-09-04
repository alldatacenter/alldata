package com.alibaba.tesla.appmanager.dynamicscript.repository;

import com.alibaba.tesla.appmanager.dynamicscript.repository.condition.DynamicScriptHistoryQueryCondition;
import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptHistoryDO;

import java.util.List;

public interface DynamicScriptHistoryRepository {

    long countByCondition(DynamicScriptHistoryQueryCondition condition);

    int deleteByCondition(DynamicScriptHistoryQueryCondition condition);

    int insert(DynamicScriptHistoryDO record);

    List<DynamicScriptHistoryDO> selectByConditionWithBLOBs(DynamicScriptHistoryQueryCondition condition);

    List<DynamicScriptHistoryDO> selectByCondition(DynamicScriptHistoryQueryCondition condition);

    int updateByConditionWithBLOBs(DynamicScriptHistoryDO record, DynamicScriptHistoryQueryCondition condition);

    int updateByCondition(DynamicScriptHistoryDO record, DynamicScriptHistoryQueryCondition condition);

    int updateByPrimaryKeyWithBLOBs(DynamicScriptHistoryDO record);

    int updateByPrimaryKey(DynamicScriptHistoryDO record);
}