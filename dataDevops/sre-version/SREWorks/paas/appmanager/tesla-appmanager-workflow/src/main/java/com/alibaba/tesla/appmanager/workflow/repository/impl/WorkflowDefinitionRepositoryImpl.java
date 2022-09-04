package com.alibaba.tesla.appmanager.workflow.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.workflow.repository.WorkflowDefinitionRepository;
import com.alibaba.tesla.appmanager.workflow.repository.condition.WorkflowDefinitionQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowDefinitionDO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowDefinitionDOExample;
import com.alibaba.tesla.appmanager.workflow.repository.mapper.WorkflowDefinitionDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class WorkflowDefinitionRepositoryImpl implements WorkflowDefinitionRepository {

    @Autowired
    private WorkflowDefinitionDOMapper mapper;

    @Override
    public long countByCondition(WorkflowDefinitionQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(WorkflowDefinitionQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(WorkflowDefinitionDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<WorkflowDefinitionDO> selectByCondition(WorkflowDefinitionQueryCondition condition) {
        condition.doPagination();
        return mapper.selectByExample(buildExample(condition));
    }

    @Override
    public int updateByCondition(WorkflowDefinitionDO record, WorkflowDefinitionQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private WorkflowDefinitionDOExample buildExample(WorkflowDefinitionQueryCondition condition) {
        WorkflowDefinitionDOExample example = new WorkflowDefinitionDOExample();
        WorkflowDefinitionDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getWorkflowType())) {
            criteria.andWorkflowTypeEqualTo(condition.getWorkflowType());
        }
        return example;
    }

    private WorkflowDefinitionDO insertDate(WorkflowDefinitionDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private WorkflowDefinitionDO updateDate(WorkflowDefinitionDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
