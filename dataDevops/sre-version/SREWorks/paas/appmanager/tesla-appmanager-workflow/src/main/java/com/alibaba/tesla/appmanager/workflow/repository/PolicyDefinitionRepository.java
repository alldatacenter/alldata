package com.alibaba.tesla.appmanager.workflow.repository;

import com.alibaba.tesla.appmanager.workflow.repository.condition.PolicyDefinitionQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.PolicyDefinitionDO;

import java.util.List;

public interface PolicyDefinitionRepository {

    long countByCondition(PolicyDefinitionQueryCondition condition);

    int deleteByCondition(PolicyDefinitionQueryCondition condition);

    int insert(PolicyDefinitionDO record);

    List<PolicyDefinitionDO> selectByCondition(PolicyDefinitionQueryCondition condition);

    int updateByCondition(PolicyDefinitionDO record, PolicyDefinitionQueryCondition condition);
}