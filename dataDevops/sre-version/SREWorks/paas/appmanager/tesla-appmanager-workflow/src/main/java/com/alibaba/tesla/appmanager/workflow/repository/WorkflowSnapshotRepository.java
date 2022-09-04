package com.alibaba.tesla.appmanager.workflow.repository;

import com.alibaba.tesla.appmanager.workflow.repository.condition.WorkflowSnapshotQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowSnapshotDO;

import java.util.List;

public interface WorkflowSnapshotRepository {

    long countByCondition(WorkflowSnapshotQueryCondition condition);

    int deleteByCondition(WorkflowSnapshotQueryCondition condition);

    int insert(WorkflowSnapshotDO record);

    List<WorkflowSnapshotDO> selectByCondition(WorkflowSnapshotQueryCondition condition);

    WorkflowSnapshotDO getByCondition(WorkflowSnapshotQueryCondition condition);

    int updateByCondition(WorkflowSnapshotDO record, WorkflowSnapshotQueryCondition condition);
}