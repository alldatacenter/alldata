package com.alibaba.tesla.appmanager.dynamicscript.repository;

import com.alibaba.tesla.appmanager.dynamicscript.repository.condition.DynamicScriptQueryCondition;
import com.alibaba.tesla.appmanager.dynamicscript.repository.domain.DynamicScriptDO;

import java.util.List;

public interface DynamicScriptRepository {

    long countByCondition(DynamicScriptQueryCondition condition);

    int deleteByCondition(DynamicScriptQueryCondition condition);

    int insert(DynamicScriptDO record);

    List<DynamicScriptDO> selectByCondition(DynamicScriptQueryCondition condition);

    DynamicScriptDO getByCondition(DynamicScriptQueryCondition condition);

    int updateByCondition(DynamicScriptDO record, DynamicScriptQueryCondition condition);

    int updateByPrimaryKey(DynamicScriptDO record);
}