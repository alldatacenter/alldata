package com.alibaba.tesla.appmanager.trait.repository.impl;

import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.alibaba.tesla.appmanager.trait.repository.TraitRepository;
import com.alibaba.tesla.appmanager.trait.repository.condition.TraitQueryCondition;
import com.alibaba.tesla.appmanager.trait.repository.domain.TraitDO;
import com.alibaba.tesla.appmanager.trait.repository.domain.TraitDOExample;
import com.alibaba.tesla.appmanager.trait.repository.mapper.TraitMapper;
import com.github.pagehelper.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class TraitRepositoryImpl implements TraitRepository {

    @Autowired
    private TraitMapper traitMapper;

    @Override
    public long countByCondition(TraitQueryCondition condition) {
        return traitMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(TraitQueryCondition condition) {
        return traitMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int deleteByPrimaryKey(Long id) {
        return traitMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(TraitDO record) {
        return traitMapper.insertSelective(insertDate(record));
    }

    @Override
    public Page<TraitDO> selectByCondition(TraitQueryCondition condition) {
        condition.doPagination();

        if(condition.isWithBlobs()){
            return traitMapper.selectByExampleWithBLOBs(buildExample(condition));
        }
        else{
            return traitMapper.selectByExample(buildExample(condition));
        }

    }

    @Override
    public TraitDO selectByPrimaryKey(Long id) {
        return traitMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByCondition(TraitDO record, TraitQueryCondition condition) {
        return traitMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByPrimaryKey(TraitDO record) {
        return traitMapper.updateByPrimaryKeySelective(updateDate(record));
    }

    private TraitDOExample buildExample(TraitQueryCondition condition) {
        TraitDOExample example = new TraitDOExample();
        TraitDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getName())) {
            criteria.andNameEqualTo(condition.getName());
        }
        if (StringUtils.isNotBlank(condition.getClassName())) {
            criteria.andClassNameEqualTo(condition.getClassName());
        }
        if (StringUtils.isNotBlank(condition.getDefinitionRef())) {
            criteria.andDefinitionRefEqualTo(condition.getDefinitionRef());
        }
        return example;
    }

    private TraitDO insertDate(TraitDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private TraitDO updateDate(TraitDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
