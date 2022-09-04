package com.alibaba.tesla.appmanager.workflow.repository.impl;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.workflow.repository.WorkflowSnapshotRepository;
import com.alibaba.tesla.appmanager.workflow.repository.condition.WorkflowSnapshotQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowSnapshotDO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowSnapshotDOExample;
import com.alibaba.tesla.appmanager.workflow.repository.mapper.WorkflowSnapshotDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class WorkflowSnapshotRepositoryImpl implements WorkflowSnapshotRepository {

    @Autowired
    private WorkflowSnapshotDOMapper mapper;

    @Override
    public long countByCondition(WorkflowSnapshotQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(WorkflowSnapshotQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(WorkflowSnapshotDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<WorkflowSnapshotDO> selectByCondition(WorkflowSnapshotQueryCondition condition) {
        condition.doPagination();
        return mapper.selectByExample(buildExample(condition));
    }

    @Override
    public WorkflowSnapshotDO getByCondition(WorkflowSnapshotQueryCondition condition) {
        List<WorkflowSnapshotDO> records = selectByCondition(condition);
        if (records.size() == 0) {
            return null;
        } else if (records.size() == 1) {
            return records.get(0);
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "multiple workflow snapshot found");
        }
    }

    @Override
    public int updateByCondition(WorkflowSnapshotDO record, WorkflowSnapshotQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private WorkflowSnapshotDOExample buildExample(WorkflowSnapshotQueryCondition condition) {
        WorkflowSnapshotDOExample example = new WorkflowSnapshotDOExample();
        WorkflowSnapshotDOExample.Criteria criteria = example.createCriteria();
        if (condition.getSnapshotId() != null && condition.getSnapshotId() > 0) {
            criteria.andIdEqualTo(condition.getSnapshotId());
        }
        if (condition.getInstanceId() != null && condition.getInstanceId() > 0) {
            criteria.andWorkflowInstanceIdEqualTo(condition.getInstanceId());
        }
        if (condition.getTaskId() != null && condition.getTaskId() > 0) {
            criteria.andWorkflowTaskIdEqualTo(condition.getTaskId());
        }
        return example;
    }

    private WorkflowSnapshotDO insertDate(WorkflowSnapshotDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private WorkflowSnapshotDO updateDate(WorkflowSnapshotDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
