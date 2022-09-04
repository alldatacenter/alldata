package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.ComponentRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.ComponentDOMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ComponentRepositoryImpl implements ComponentRepository {

    @Autowired
    private ComponentDOMapper mapper;

    @Override
    public long countByCondition(ComponentQueryCondition condition) {
        return mapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(ComponentQueryCondition condition) {
        return mapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(ComponentDO record) {
        return mapper.insertSelective(insertDate(record));
    }

    @Override
    public List<ComponentDO> selectByCondition(ComponentQueryCondition condition) {
        if (condition.isWithBlobs()) {
            return mapper.selectByExampleWithBLOBs(buildExample(condition));
        } else {
            return mapper.selectByExample(buildExample(condition));
        }
    }

    @Override
    public int updateByCondition(ComponentDO record, ComponentQueryCondition condition) {
        return mapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private ComponentDOExample buildExample(ComponentQueryCondition condition) {
        ComponentDOExample example = new ComponentDOExample();
        ComponentDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getComponentType())) {
            criteria.andComponentTypeEqualTo(condition.getComponentType());
        }
        if (StringUtils.isNotBlank(condition.getComponentAdapterType())) {
            criteria.andComponentAdapterTypeEqualTo(condition.getComponentAdapterType());
        }
        return example;
    }

    private ComponentDO insertDate(ComponentDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private ComponentDO updateDate(ComponentDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
