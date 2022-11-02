package com.alibaba.tesla.appmanager.workflow.repository.impl;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.workflow.repository.WorkflowInstanceRepository;
import com.alibaba.tesla.appmanager.workflow.repository.condition.WorkflowInstanceQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowInstanceDO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowInstanceDOExample;
import com.alibaba.tesla.appmanager.workflow.repository.mapper.WorkflowInstanceDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class WorkflowInstanceRepositoryImpl implements WorkflowInstanceRepository {

    @Autowired
    private WorkflowInstanceDOMapper mapper;

    @Override
    public long countByCondition(WorkflowInstanceQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(WorkflowInstanceQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(WorkflowInstanceDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public WorkflowInstanceDO getByCondition(WorkflowInstanceQueryCondition condition) {
        List<WorkflowInstanceDO> records = selectByCondition(condition);
        if (records.size() == 0) {
            return null;
        } else if (records.size() == 1) {
            return records.get(0);
        } else {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, "multiple workflow instance found");
        }
    }

    @Override
    public List<WorkflowInstanceDO> selectByCondition(WorkflowInstanceQueryCondition condition) {
        condition.doPagination();
        if (condition.isWithBlobs()) {
            return mapper.selectByExampleWithBLOBs(buildExample(condition));
        } else {
            return mapper.selectByExample(buildExample(condition));
        }
    }

    @Override
    public int updateByCondition(WorkflowInstanceDO record, WorkflowInstanceQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByPrimaryKey(WorkflowInstanceDO record) {
        return mapper.updateByPrimaryKeySelective(updateDate(record));
    }

    private WorkflowInstanceDOExample buildExample(WorkflowInstanceQueryCondition condition) {
        WorkflowInstanceDOExample example = new WorkflowInstanceDOExample();
        WorkflowInstanceDOExample.Criteria criteria = example.createCriteria();
        if (condition.getInstanceId() != null && condition.getInstanceId() > 0) {
            criteria.andIdEqualTo(condition.getInstanceId());
        }
        if (StringUtils.isNotBlank(condition.getAppId())) {
            criteria.andAppIdEqualTo(condition.getAppId());
        }
        if (StringUtils.isNotBlank(condition.getWorkflowStatus())) {
            criteria.andWorkflowStatusEqualTo(condition.getWorkflowStatus());
        }
        if (StringUtils.isNotBlank(condition.getWorkflowCreator())) {
            criteria.andWorkflowCreatorEqualTo(condition.getWorkflowCreator());
        }
        return example;
    }

    private WorkflowInstanceDO insertDate(WorkflowInstanceDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private WorkflowInstanceDO updateDate(WorkflowInstanceDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
