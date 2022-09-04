package com.alibaba.tesla.appmanager.workflow.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.workflow.repository.PolicyDefinitionRepository;
import com.alibaba.tesla.appmanager.workflow.repository.condition.PolicyDefinitionQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.PolicyDefinitionDO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.PolicyDefinitionDOExample;
import com.alibaba.tesla.appmanager.workflow.repository.mapper.PolicyDefinitionDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class PolicyDefinitionRepositoryImpl implements PolicyDefinitionRepository {

    @Autowired
    private PolicyDefinitionDOMapper mapper;

    @Override
    public long countByCondition(PolicyDefinitionQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(PolicyDefinitionQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(PolicyDefinitionDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<PolicyDefinitionDO> selectByCondition(PolicyDefinitionQueryCondition condition) {
        condition.doPagination();
        return mapper.selectByExample(buildExample(condition));
    }

    @Override
    public int updateByCondition(PolicyDefinitionDO record, PolicyDefinitionQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private PolicyDefinitionDOExample buildExample(PolicyDefinitionQueryCondition condition) {
        PolicyDefinitionDOExample example = new PolicyDefinitionDOExample();
        PolicyDefinitionDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getPolicyType())) {
            criteria.andPolicyTypeEqualTo(condition.getPolicyType());
        }
        return example;
    }

    private PolicyDefinitionDO insertDate(PolicyDefinitionDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private PolicyDefinitionDO updateDate(PolicyDefinitionDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
