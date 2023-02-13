package com.alibaba.tesla.appmanager.server.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.server.repository.UnitRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.UnitQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.UnitDO;
import com.alibaba.tesla.appmanager.server.repository.domain.UnitDOExample;
import com.alibaba.tesla.appmanager.server.repository.mapper.UnitDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class UnitRepositoryImpl implements UnitRepository {

    @Autowired
    private UnitDOMapper unitDOMapper;

    @Override
    public long countByCondition(UnitQueryCondition condition) {
        return unitDOMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(UnitQueryCondition condition) {
        return unitDOMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int insert(UnitDO record) {
        return unitDOMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<UnitDO> selectByCondition(UnitQueryCondition condition) {
        return unitDOMapper.selectByExample(buildExample(condition));
    }

    @Override
    public int updateByCondition(UnitDO record, UnitQueryCondition condition) {
        return unitDOMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    private UnitDOExample buildExample(UnitQueryCondition condition) {
        UnitDOExample example = new UnitDOExample();
        UnitDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getUnitId())) {
            criteria.andUnitIdEqualTo(condition.getUnitId());
        }
        if (StringUtils.isNotBlank(condition.getUnitName())) {
            criteria.andUnitNameEqualTo(condition.getUnitName());
        }
        if (StringUtils.isNotBlank(condition.getCategory())) {
            criteria.andCategoryEqualTo(condition.getCategory());
        }
        return example;
    }

    private UnitDO insertDate(UnitDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private UnitDO updateDate(UnitDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
