package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.ComponentHistoryRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentHistoryQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentHistoryDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentHistoryDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.ComponentHistoryDOMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ComponentHistoryRepositoryImpl implements ComponentHistoryRepository {

    @Autowired
    private ComponentHistoryDOMapper mapper;

    @Override
    public long countByCondition(ComponentHistoryQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(ComponentHistoryQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(ComponentHistoryDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<ComponentHistoryDO> selectByConditionWithBLOBs(ComponentHistoryQueryCondition condition) {
        return mapper.selectByExampleWithBLOBs(buildExample(condition));
    }

    @Override
    public List<ComponentHistoryDO> selectByCondition(ComponentHistoryQueryCondition condition) {
        return mapper.selectByExample(buildExample(condition));
    }

    @Override
    public int updateByConditionWithBLOBs(ComponentHistoryDO record, ComponentHistoryQueryCondition condition) {
        return mapper.updateByExampleWithBLOBs(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByCondition(ComponentHistoryDO record, ComponentHistoryQueryCondition condition) {
        return mapper.updateByExample(updateDate(record), buildExample(condition));
    }

    private ComponentHistoryDOExample buildExample(ComponentHistoryQueryCondition condition) {
        ComponentHistoryDOExample example = new ComponentHistoryDOExample();
        ComponentHistoryDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getComponentType())) {
            criteria.andComponentTypeEqualTo(condition.getComponentType());
        }
        if (StringUtils.isNotBlank(condition.getComponentAdapterType())) {
            criteria.andComponentAdapterTypeEqualTo(condition.getComponentAdapterType());
        }
        if (condition.getRevision() != null && condition.getRevision() > 0) {
            criteria.andRevisionEqualTo(condition.getRevision());
        }
        return example;
    }

    private ComponentHistoryDO insertDate(ComponentHistoryDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private ComponentHistoryDO updateDate(ComponentHistoryDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
