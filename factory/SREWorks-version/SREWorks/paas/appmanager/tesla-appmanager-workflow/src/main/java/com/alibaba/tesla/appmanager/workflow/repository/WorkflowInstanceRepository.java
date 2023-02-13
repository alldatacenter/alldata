package com.alibaba.tesla.appmanager.workflow.repository;

import com.alibaba.tesla.appmanager.workflow.repository.condition.WorkflowInstanceQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowInstanceDO;

import java.util.List;

public interface WorkflowInstanceRepository {

    long countByCondition(WorkflowInstanceQueryCondition condition);

    int deleteByCondition(WorkflowInstanceQueryCondition condition);

    int insert(WorkflowInstanceDO record);

    WorkflowInstanceDO getByCondition(WorkflowInstanceQueryCondition condition);

    List<WorkflowInstanceDO> selectByCondition(WorkflowInstanceQueryCondition condition);

    int updateByCondition(WorkflowInstanceDO record, WorkflowInstanceQueryCondition condition);

    int updateByPrimaryKey(WorkflowInstanceDO record);
}