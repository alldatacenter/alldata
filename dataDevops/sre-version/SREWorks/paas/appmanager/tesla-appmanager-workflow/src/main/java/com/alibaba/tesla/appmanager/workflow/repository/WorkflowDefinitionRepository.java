package com.alibaba.tesla.appmanager.workflow.repository;

import com.alibaba.tesla.appmanager.workflow.repository.condition.WorkflowDefinitionQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowDefinitionDO;

import java.util.List;

public interface WorkflowDefinitionRepository {

    long countByCondition(WorkflowDefinitionQueryCondition condition);

    int deleteByCondition(WorkflowDefinitionQueryCondition condition);

    int insert(WorkflowDefinitionDO record);

    List<WorkflowDefinitionDO> selectByCondition(WorkflowDefinitionQueryCondition condition);

    int updateByCondition(WorkflowDefinitionDO record, WorkflowDefinitionQueryCondition condition);
}